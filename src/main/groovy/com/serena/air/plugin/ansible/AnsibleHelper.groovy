package com.serena.air.plugin.ansible

import com.urbancode.air.CommandHelper
import com.urbancode.air.ExitCodeException

class AnsibleHelper {
    String home
    File workdir
    boolean debug = false
    private String output
    private CommandHelper ch

    /**
     * Default constructor
     * @param workdir The working directory for the CommandHelper CLI
     * @param home The full path to the "ansible" script location
     */
    AnsibleHelper(File workdir, String home) {
        if (home) {
            this.home = home
        } else {
            this.home = "ansible"
        }
        if (workdir) {
            this.workdir = workdir
        }
        ch = new CommandHelper(workdir)
        ArrayList args = []
        args << '--version'
        if (!runCommand("Checking '${this.home}' exists...", args)) {
            System.exit(1)
        }
    }

    String getHome() {
        return this.home
    }

    def setHome(String home) {
        this.home = home
    }

    String getOutput() {
        return this.output
    }

    def setOutput(String output) {
        this.output = output
    }

    boolean getDebug() {
        return this.debug
    }

    def setDebug(boolean debug) {
        this.debug = debug
    }

    /**
     * @param args An ArrayList of arguments to be executed by the command prompt
     * @return true if the command is run without any Standard Errors, false otherwise
     */
    public boolean runCommand(message, ArrayList args) {
        ArrayList cmd = [home]
        args.each() { arg ->
            cmd << arg
        }
        boolean status
        try {
            ch.runCommand("[INFO] ${message}", cmd) { Process proc ->
                def (String out, String err) = captureCommand(proc)
                setOutput(out)
                if (err) {
                    error(err)
                    status = false
                } else {
                    if (args.size() > 0) {
                        info("Command output:\n${out}")
                    }
                    status = true
                }
            }
        } catch (ExitCodeException ex) {
            error(ex.toString())
            return false
        }
        return status
    }

    /**
     * @param args The list of arguments to add the configuration options to
     * @param limit Limit execution to matching hosts
     * @param remoteUser The remote user to run as
     * @param become Become the default user (Root)
     * @param becomeUser Become a specific user
     * @param inventory An alternative inventory file to use
     * @param privateKey The private key file to use
     * @param options Additional options
     */
    public void setOptions(ArrayList args, String limit, String remoteUser, Boolean become, String becomeUser,
                           String inventory, String privateKey, String extraVars, String options, Boolean verbose) {
        if (!isEmpty(limit)) {
            args << '-l'
            args << "'" + limit + "'"
        }
        if (!isEmpty(remoteUser)) {
            args << '-u'
            args << remoteUser
        }
        if (become && isEmpty(becomeUser)) {
            args << '-b'
        }
        if (!isEmpty(becomeUser) && !become) {
            args << '--become-user=' + becomeUser
        }
        if (!isEmpty(inventory)) {
            args << '-i'
            args << inventory
        }
        if (!isEmpty(privateKey)) {
            args << '--private-key=' + privateKey
        }
        if (!isEmpty(extraVars)) {
            extraVars.split("(?<=(^|[^\\\\])(\\\\{2}){0,8}),").each { prop ->
                //split out the name
                def parts = prop.split("(?<=(^|[^\\\\])(\\\\{2}){0,8})=", 2);
                def propName = parts[0];
                def propValue = parts.size() == 2 ? parts[1] : "";
                //replace \, with just , and then \\ with \
                propName = propName.replace("\\=", "=").replace("\\,", ",").replace("\\\\", "\\")
                propValue = propValue.replace("\\=", "=").replace("\\,", ",").replace("\\\\", "\\")
                args << '-e'
                args << "'" + propName + '=' + propValue + "'"
                debug("setting environment variable: '${propName}=${propValue}'")
            }
        }
        if (!isEmpty(options)) {
            options.split("[\r\n]+").each() { option ->
                args << option
            }
        }
        if (verbose) {
            args << '-vvvv'
            setDebug(true)
        }
    }

    /**
     * Check if a string is null, empty, or all whitespace
     * @param str The string whose value to check
     */
    public boolean isEmpty(String str) {
        return (str == null) || str.trim().isEmpty();
    }

    // ----------------------------------------

    /**
     * @param proc The process to retrieve the standard output and standard error from
     * @return An array containing the standard output and standard error of the process
     */
    private String[] captureCommand(Process proc) {
        StringBuffer out = new StringBuffer()
        StringBuffer err = new StringBuffer()
        proc.waitForProcessOutput(out, err)
        proc.out.close()
        return [out.toString(), err.toString()]
    }

    private debug(String message) {
        if (this.debug) {
            println("[DEBUG] ${message}")
        }
    }

    private info(String message) {
        println("[INFO] ${message}")
    }

    private error(String message) {
        println("[ERROR] ${message}")
    }
}
