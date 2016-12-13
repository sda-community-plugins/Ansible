import com.serena.air.http.SDAHttpClientHelper
import com.serena.air.plugin.ansible.AnsibleHelper
import com.urbancode.air.AirPluginTool
import com.urbancode.air.xtrust.XTrustProvider
import com.urbancode.ud.client.UDRestClient
import org.codehaus.jettison.json.JSONObject

final def apTool = new AirPluginTool(this.args[0], this.args[1])
final def props = apTool.getStepProperties()
final def udUser = apTool.getAuthTokenUsername()
final def udPass = apTool.getAuthToken()
final def weburl = System.getenv("AH_WEB_URL")
final def client = new UDRestClient(new URI(weburl), SDAHttpClientHelper.getHttpClient(udUser, udPass))
XTrustProvider.install()

final def parentResource = props['parentResource']?.trim()
final def resourceGroup = props['resourceGroup']?.trim()
final def hostPattern = props['hostPattern']?.trim()
final def inventoryFile = props["inventoryFile"]?.trim()
final def path = props['path']?.trim()
final def verbose = props["verbose"]?.trim().toBoolean()

AnsibleHelper helper = new AnsibleHelper(new File(".").canonicalFile,
        (path == null) || path.trim().isEmpty() ? "ansible" : path)

ArrayList args = []

if (helper.isEmpty(inventoryFile)) {
    args << 'all'
} else {
    args << hostPattern
}
args << '--list-hosts'
if (!helper.isEmpty(inventoryFile)) {
    args << '-i'
    args << inventoryFile
}
if (verbose) {
    args << '-vv'
    helper.setDebug(true)
}

if (helper.runCommand("Finding hosts to import...", args)) {

    def resources = helper.getOutput()

    try {
        JSONObject resourceJSON = client.getResourceByName(parentResource);
        if (resourceJSON == null) {
            throw new IOException("Parent resource not found.")
        }
        helper.info("Found Parent Resource with name '${parentResource}'.")
    } catch (IOException e) {
        if (e.getMessage().contains("404") || e.getMessage().contains("no resource found") || e.getMessage().contains("No resource with")) {
            helper.error("The request was successful but no resource with name '${resourceName}' was found.")
            System.exit(0)
        } else {
            helper.error("An error occurred during your request:")
            throw new IOException(e)
        }
    }

    def description = "Created automatically from Ansible plugin."
    resources.eachLine() {
        def newResource
        def res = it.replaceAll("\\s", "")
        try {
            JSONObject resourceJSON = client.getResourceByName(res);
            if (resourceJSON != null) {
                helper.info("Resource with name '${res}' already exists, not recreating...")
            } else {
                helper.info("Creating Resource with name '${res}'.")
                newResource = client.createResource(res, description, "parentResource", parentResource)
                helper.info("Adding Resource '${newResource}' to Resource Group '${resourceGroup}'")
                client.addResourceToGroup(newResource, resourceGroup)
            }
        } catch (IOException e) {
            if (e.getMessage().contains("404") || e.getMessage().contains("no resource found") || e.getMessage().contains("No resource with")) {
                helper.error("A 404 error occured: ${e.getMessage().toString()}")

            } else {
                helper.error("An error occurred during your request:")
                throw new IOException(e)
            }
        }
    }

    System.exit(0)
} else {
    System.exit(1)
}
