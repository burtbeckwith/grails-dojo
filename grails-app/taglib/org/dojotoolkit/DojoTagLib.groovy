package org.dojotoolkit

import grails.converters.JSON

class DojoTagLib {

  static namespace = "dojo"

  /**
   * @return the dojo.customBuild value from Config.groovy
   */
  private boolean useCustomDojoJsBuild() {
    def includeCustomScripts = grailsApplication.config.dojo.use.customBuild.js ?: false
    return includeCustomScripts
  }

  /**
   * @return the dojo.customBuild value from Config.groovy
   */
  private boolean useCustomDojoCssBuild() {
    def includeCustomCss = (grailsApplication?.config?.css?.size()) ?: (grailsApplication?.config?.dojo?.use?.customBuild?.css) ?: false
    return includeCustomCss
  }

  /**
   * Reads the dojo.profile.js file and converts into a Grails object
   * @return JSONObject
   */
  private Map getDojoCustomProfile() {
    def jsonString = grailsApplication.config.dojo.profile
    def jsonObj = JSON.parse("{${jsonString}}")
    return jsonObj
  }

  /**
   * @return the dojo home based on if it has a custom build
   */
  private String dojoHome() {
    def dojoHome = "${g.resource(dir: pluginContextPath)}/js/dojo/${Dojo.version}"
    def customDojo = "${g.resource(dir:'')}/js/dojo/${Dojo.pluginVersion}-custom"

    if (useCustomDojoJsBuild()) {
      return customDojo
    }
    else {
      return dojoHome
    }
  }

  /**
   * Allows other tags to get the dojo home as ${dojo.home()}
   */
  def home = {
    out << dojoHome()
  }

  /**
   * Outputs custom js scripts that were created as part of the custom dojo build.
   * Checks if dojo.include.custombuild.inHeader is true.
   */
  def customDojoScripts = {
    if (useCustomDojoJsBuild()) {
      def profileObj = getDojoCustomProfile()

      // For Dojo 1.7 profiles (AMD Module Based)
      // Example http://svn.dojotoolkit.org/src/util/trunk/buildscripts/profiles/amd.profile.js
      profileObj["var profile"]?.layers?.each{k,v->
        out << "<script type='text/javascript' src='${dojoHome()}/${k}.js'></script>"
      }

      // For Dojo 1.6 profiles
      // Example: http://livedocs.dojotoolkit.org/build/pre17/build#profiles
      profileObj?.dependencies?.layers?.each {
        out << "<script type='text/javascript' src='${dojoHome()}/dojo/${it.name}'></script>"
      }
    }
  }

  /**
   * Alternative to <g:javascript library="dojo"/>. Includes the dojo.js file,
   * adds the standard dojo headers., and sets the theme.
   *
   * @param attrs.require = This is a map of components to include
   * @param attrs.theme = (optional) Includes the theme if it is provided
   * @param attrs.includeCustomBuild = (true) Includes the js files(layers) defined in dojo.profile.js.
   *                                    It is recommended you leave this to true. Setting to false, you will
   *                                    have to manually include the generated files yourself but it give more
   *                                    fine grain control on when the files get included.
   *
   * @param attrs.async = Boolean (true) If false will use the loader in non-AMD mode.
   * @param attrs.modules = List (optional) A list of required modules to be included. Just calles require().
   * @param attrs.modulePaths = List (optional) A list of paths to search for required modules.
   */
  def header = {attrs ->
    // Standard Dojo Config Settings (and defaults)
    attrs.isDebug = attrs.isDebug ?: "false"
    attrs.parseOnLoad = attrs.parseOnLoad ?: "true"
    attrs.async = attrs.async ?: "true"


    // Custom Properties for Dojo Plugin
    def includeCustomBuild = attrs.remove("includeCustomBuild") ?: "true"
    def showSpinner = attrs.remove("showSpinner") ?: "true"
    def modulePaths = attrs.remove("modulePaths") ?: [:]
    def modules = attrs.remove("modules") ?: []
    def theme = attrs.remove("theme") ?: "tundra"
    def jsRoot = "${resource()}/js"

    // Add custom tags space to modulePath (Append new path to be relative to plugin's version of dojo.js
    def moduleStringList = []
	  def paths = []
    modulePaths?.each{k,v->
      moduleStringList.add("'${k}':'${jsRoot}/${v}'")
	  paths.add("'${k}':'${v}'")
    }
    //Add DojoUI Module Path
    moduleStringList.add("'dojoui':'../dojoui'")
	paths.add("'dojoui':'dojo/${Dojo.version}/dojoui'")
	paths.add("'dojo':'dojo/${Dojo.version}/dojo'")
	paths.add("'dijit':'dojo/${Dojo.version}/dijit'")
	paths.add("'dojox':'dojo/${Dojo.version}/dojox'")

    def dojoConfig = attrs.collect{ "${it.key}:${it.value}" }.join(', ')

    if (theme) {
      out << stylesheets([theme:theme])
    }

    // New Dojo AMD Loader
    if (attrs.async == "true") {
      out << """
        <script>
          dojoConfig = {${dojoConfig}, paths:{${paths.join(',')}}, baseUrl : '${jsRoot}' };
          dojoGrailsPluginConfig = {showSpinner:${showSpinner} };
        </script>
        <script type='text/javascript' src='${dojoHome()}/dojo/dojo.js'></script>
      """
    }
    // Use old Dojo loader
    else {
      out << """
        <script>
          dojoConfig = {${dojoConfig}, modulePaths:{ ${moduleStringList.join(',')}} };
          dojoGrailsPluginConfig = {showSpinner:${showSpinner} };
        </script>
        <script type='text/javascript' src='${dojoHome()}/dojo/dojo.js.uncompressed.js'></script>
      """
    }
    if(showSpinner == "true"){
      out << """ <script type='text/javascript' src='${dojoHome()}/dojoui/DojoGrailsSpinner.js'></script> """
    }

    // if custom build then include released js files
    if(includeCustomBuild == "true"){
      out << customDojoScripts()
    }
    if (modules?.size()) {
      out << require([async:attrs.async, modules:modules])
    }
  }

  /**
   * Sets up the base css and themes.User still needs to define <body class="${theme}">
   * @param attrs.theme  = (Tundra), Soria, Nihilio. The theme to bring in.
   */
  def stylesheets = {attrs ->
    def theme = attrs.remove("theme") ?: "tundra"
    if(useCustomDojoCssBuild()){
      out << """
          <link rel="stylesheet" type="text/css" href="${dojoHome()}/css/custom-dojo.css" />
          <!--[if lt IE 8]>
            <link rel="stylesheet" type="text/css" href="${dojoHome()}/dojoui/resources/css/dojo-ui-ie.css" />
          <![endif]-->
      """
    }
    else{
      out << """
        <link rel="stylesheet" type="text/css" href="${dojoHome()}/dojo/resources/dojo.css" />
        <link rel="stylesheet" type="text/css" href="${dojoHome()}/dijit/themes/dijit.css" />
        <link rel="stylesheet" type="text/css" href="${dojoHome()}/dijit/themes/${theme}/${theme}.css" />
        <link rel="stylesheet" type="text/css" href="${dojoHome()}/dojoui/resources/css/dojo-ui.css" />
        <!--[if lt IE 8]>
          <link rel="stylesheet" type="text/css" href="${dojoHome()}/dojoui/resources/css/dojo-ui-ie.css" />
        <![endif]-->
      """
    }
  }

  /**
   * Includes a dojo specific css file. This is used mostly for extended css files in dojox.
   * Please use <dojo:header> or <dojo:stylesheets> for the standard files.
   */
  def css = {attrs ->
    out << "<link rel='stylesheet' type='text/css' href='${dojoHome()}/${attrs?.file}'/>"
  }

  /**
   * Includes dojo modules via the dojo loader and make them available in the body of the tag script
   * Each require provides a callback parameter named after the last part of the module name
   * e.g.: "dijit/form/Form" provides a parameter called "Form" in the callback
   * @param attrs.modules = This is a map of components to include
   * @param attrs.callbackParamNames = This overrides the default callback parameter names, in case you want to use different ones than the defaults
   * @param attrs.wrapperFunction = put the require in a wrapper function (include its signature). e.g: myFunction(param1,param2)
   */
  def require = {attrs, body ->
	  def modules = attrs?.modules?.collect{ "'${it}'" }
	  def callbacks = attrs?.modules?.collect{ it.split("/").last() }
	  if (attrs?.callbackParamNames) {
		  callbacks = attrs.callbackParamNames
	  }
	  assert attrs?.callbackParamNames?.size() <= modules.size()

	  out << """
		<script type='text/javascript'>
		  """
	  if (attrs?.wrapperFunction) {
		  out << "function ${attrs?.wrapperFunction}{"
	  }
      out << """
		  	require(${modules},
		  		function(${callbacks.join(',')}){
	  """
		attrs?.modulePaths?.each{k,v->
		  out << " dojo.registerModulePath('${k}','${v}'); "
		}

	  out << body()
	  out << """
	  		});
	  """

	  if (attrs?.wrapperFunction) {
		  out << "}" // wrapper function
	  }
	  out << """
	  </script>
	  """
  }

  /**
   * Wraps a response in a text area element if a flash var is set.
   * This is set by a dojo.io.iframe call and intercepted by the BaseController.
   * The end result is that a user is able to do a file upload via an ajax call
   * inside of a ContentPane.
   *
   * Add this code as a controller filter:
      def filters = {
        parameterFilter(controller: '*', action: '*') {
          before = {
            if (params?.dojoIoIframeTransport) { flash.dojoIoIframeTransport = true }
          }
        }
      }
   *
   */
  def ajaxUploadResponse = {attrs, body ->
    if (flash?.dojoIoIframeTransport) {
      out << "<textarea>${body()}</textarea>"
      flash.remove("dojoIoIframeTransport")
    }
    else {
      out << body()
    }
  }
}
