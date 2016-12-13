import com.serena.air.plugin.ansible.AnsibleHelper
import com.urbancode.air.AirPluginTool

final def apTool = new AirPluginTool(this.args[0], this.args[1])
final def props = apTool.getStepProperties()

final def hostPattern = props['hostPattern']?.trim()
final def limit = props["limit"]?.trim()
final def moduleName = props["moduleName"]?.trim()
final def moduleArgs = props["moduleArgs"]?.trim()
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
        (path == null) || path.trim().isEmpty() ? "ansible" : path)

ArrayList args = []
helper.setOptions(args, limit, remoteUser, become, becomeUser, inventoryFile, privateKeyFile, extraVars, options, verbose)

if (!helper.isEmpty(hostPattern)) {
    args << hostPattern
} else {
    args << 'all'
}
if (!helper.isEmpty(moduleName)) {
    args << '-m'
    args << moduleName
    args << '-a'
    args << "'" + moduleArgs + "'"
} else {
    args << '-a'
    args << "'" + moduleArgs + "'"
}

if (helper.runCommand("Executing ansible...", args)) {
    System.exit(0)
} else {
    System.exit(1)
}
