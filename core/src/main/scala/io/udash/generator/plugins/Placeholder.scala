package io.udash.generator.plugins

sealed class Placeholder(str: String) {
  override def toString: String = s"/*<<$str>>*/"
}

case object UdashBuildPlaceholder               extends Placeholder("udash-generator-custom-build")

case object DependenciesPlaceholder             extends Placeholder("udash-generator-dependencies")
case object DependenciesVariablesPlaceholder    extends Placeholder("udash-generator-dependencies-variables")
case object DependenciesFrontendPlaceholder     extends Placeholder("udash-generator-dependencies-frontend")
case object DependenciesFrontendJSPlaceholder   extends Placeholder("udash-generator-dependencies-frontendJS")
case object DependenciesCrossPlaceholder        extends Placeholder("udash-generator-dependencies-cross")
case object DependenciesBackendPlaceholder      extends Placeholder("udash-generator-dependencies-backend")

case object RootSettingsPlaceholder             extends Placeholder("udash-generator-root-settings")
case object RootModulePlaceholder               extends Placeholder("udash-generator-root-module")
case object FrontendSettingsPlaceholder         extends Placeholder("udash-generator-frontend-settings")
case object FrontendModulePlaceholder           extends Placeholder("udash-generator-frontend-module")
case object BackendSettingsPlaceholder          extends Placeholder("udash-generator-backend-settings")
case object BackendModulePlaceholder            extends Placeholder("udash-generator-backend-module")
case object SharedSettingsPlaceholder           extends Placeholder("udash-generator-shared-settings")
case object SharedModulePlaceholder             extends Placeholder("udash-generator-shared-module")
case object SharedJSModulePlaceholder           extends Placeholder("udash-generator-sharedJS-module")
case object SharedJVMModulePlaceholder          extends Placeholder("udash-generator-sharedJVM-module")

case object HTMLHeadPlaceholder                 extends Placeholder("udash-generator-html-head")

case object FrontendRoutingRegistryPlaceholder  extends Placeholder("udash-generator-frontend-routing-registry")
case object FrontendVPRegistryPlaceholder       extends Placeholder("udash-generator-frontend-vp-registry")
case object FrontendStatesRegistryPlaceholder   extends Placeholder("udash-generator-frontend-states-registry")
case object FrontendIndexMenuPlaceholder        extends Placeholder("udash-generator-frontend-index-menu")
case object FrontendContextPlaceholder          extends Placeholder("udash-generator-frontend-context")
case object FrontendAppInitPlaceholder          extends Placeholder("udash-generator-frontend-app-init")
case object FrontendImportsPlaceholder          extends Placeholder("udash-generator-frontend-imports")

case object FrontendStyledHeaderPlaceholder     extends Placeholder("udash-generator-frontend-styled-header")
case object FrontendStyledFooterPlaceholder     extends Placeholder("udash-generator-frontend-styled-footer")

case object FrontendStylesMainPlaceholder       extends Placeholder("udash-generator-frontend-styles-main")
case object FrontendStylesBodyPlaceHolder       extends Placeholder("udash-generator-frontend-styles-body")
case object FrontendStylesLinkBlackPlaceholder  extends Placeholder("udash-generator-frontend-styles-link-black")
case object FrontendStylesStepsListPlaceholder  extends Placeholder("udash-generator-frontend-styles-steps-list")

case object BackendAppServerPlaceholder         extends Placeholder("udash-generator-backend-app-server")
