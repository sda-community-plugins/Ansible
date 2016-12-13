import com.serena.air.plugin.ansible.AnsibleHelper
import com.urbancode.air.AirPluginTool

final def apTool = new AirPluginTool(this.args[0], this.args[1])
final def props = apTool.getStepProperties()

final def limit = props["limit"]?.trim()
final def playbookScript = props["playbookScript"]?.trim()
final def tags = props["tags"]?.trim()
final def skipTags = props["skipTags"]?.trim()
final def startAtTask = props["startAtTask"]?.trim()
final def remoteUser = props["remoteUser"]?.trim()
final def become = props["become"]?.trim().toBoolean()
final def becomeUser = props["becomeUser"]?.trim()
final def inventoryFile = props["inventoryFile"]?.trim()
final def privateKeyFile = props["privateKeyFile"]?.trim()
final def extraVars = props["extraVars"]?.trim()
final def options = props["options"]?.trim()
final def path = props['path']?.trim()
final def verbose = props["verbose"]?.trim().toBoolean()

AnsibleHelper helper = new AnsibleHelper(new File(".").canonicalFile,
        (path == null) || path.trim().isEmpty() ? "ansible-playbook" : path)

ArrayList args = []
helper.setOptions(args, limit, remoteUser, become, becomeUser, inventoryFile, privateKeyFile, extraVars, options, verbose)

if (helper.isEmpty(playbookScript)) {
    helper.debug("Playbook script is empty.")
    System.exit(1)
} else {
    File temp = File.createTempFile(UUID.randomUUID().toString() + "playbook", ".yaml")
    temp.write(playbookScript.toString())
    helper.debug("Created temp file ${temp.getAbsolutePath()}.")
    args << temp.getAbsolutePath()
}
if (!helper.isEmpty(tags)) {
    args << '--tags=\'' + tags + '\''
}
if (!helper.isEmpty(skipTags)) {
    args << '--skip-tags=\'' + tags + '\''
}
if (!helper.isEmpty(startAtTask)) {
    args << '--start-at-task=\'' + startAtTask + '\''
}

if (helper.runCommand("Executing playbook...", args)) {
    System.exit(0)
} else {
    System.exit(1)
}

