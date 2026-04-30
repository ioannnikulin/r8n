rootProject.name = "r8n-backend"

includeBuild("build-logic")
include("platform")
include("core")

include("gateway-sv", "gateway-sv/service")

include("mock-api", "mock-sv/api")
include("mock-api-integration", "mock-sv/api-integration")
include("mock-client", "mock-sv/client")
include("mock-sv", "mock-sv/service")

include("messaging-api", "messaging-sv/api")
include("messaging-api-integration", "messaging-sv/api-integration")
include("messaging-client", "messaging-sv/client")
include("messaging-sv", "messaging-sv/service")

include("users-api", "users-sv/api")
include("users-api-integration", "users-sv/api-integration")
include("users-client", "users-sv/client")
include("users-sv", "users-sv/service")

include("export-api", "export-sv/api")
include("export-sv", "export-sv/service")

include("opinions-api", "opinions-sv/api")
include("opinions-api-integration", "opinions-sv/api-integration")
include("opinions-client", "opinions-sv/client")
include("opinions-sv", "opinions-sv/service")

include("core:security-common")
include("core:security-servlet")
include("core:security-reactive")
include("core:api")
include("core:utils")
include("core:web")

fun include(name: String, projectDir: String) {
    include(name)
    project(":$name").projectDir = file(projectDir)
}
