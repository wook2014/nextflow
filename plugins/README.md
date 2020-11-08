# Nextflow plugins

This directory should contain plugin subprojects for Nextflow. 

The `build.gradle` defines the main actions for each plugin.

## Plugin subproject structure 
 
### Plugin structure 

The plugin subproject defines its own `build.gradle` and setup the required dependencies. 

Minimal dependencies shown below: 

``` 
dependencies {
    compileOnly project(':nextflow')
    compileOnly 'org.slf4j:slf4j-api:1.7.10'
    compileOnly 'org.pf4j:pf4j:3.4.1'

    testImplementation project(':nextflow')
    testImplementation "org.codehaus.groovy:groovy:3.0.5"
    testImplementation "org.codehaus.groovy:groovy-nio:3.0.5"
}
``` 

Each plugin subproject directory name has to begin with the prefix `nf-` and must include 
a file named `src/resources/META-INF/MANIFEST.MF` which contains the plugin metadata. 
The manifest content looks like the following:

```
Manifest-Version: 1.0
Plugin-Class: the.plugin.ClassName
Plugin-Id: the-plugin-id
Plugin-Provider: Some Provider Name
Plugin-Version: 0.0.0
```   
  
## Environment variables 

* `NXF_PLUGINS_MODE`: Define the plugin system execution mode, either *prod* for production or *dev* for development
  (see below for details).  
* `NXF_PLUGINS_DIR`: the path where the plugins archives are stored/loaded. Default is `$NXF_HOME/plugins` for 
  production mode and `$PWD/plugins` for dev mode. 
* `NXF_PLUGINS_DEFAULT`: Whenever use the default plugins when no plugin is specified in the config file.   

## Development environment

When running in development the plugin system uses the `GroovyDevPluginClasspath` to load plugins classes 
from each plugin project build path e.g. `$PWD/plugins/nf-amazon/build/classes` and 
`$PWD/plugins/nf-amazon/build/target/libs` (for deps libraries).    


## The plugins repository 

The plugins meta-info are published via a GitHub repository at [https://github.com/nextflow-io/plugins]()
and accessible through the URL [https://raw.githubusercontent.com/nextflow-io/plugins/main/plugins.json]().

The repository index has the following structure: 

```
[
  {
    "id": "nf-somthing",
    "releases": [
      {
        "version": "0.1.0",
        "url": "http://www.nextflow.io.s3-website-eu-west-1.amazonaws.com/plugins/nf-something/nf-something-0.1.0.zip",
        "date": "2020-10-12T10:05:44.28+02:00",
        "sha512sum": "9e9e33695c1a7c051271..."
      }
    ]
  },
  :
]
```

## Gradle Tasks 

### makeZip
    
Creates the plugin the zip file and the json meta file in the
subproject `build/libs` directory.

```
» ls -l1 $PWD/plugins/nf-tower/build/libs/
nf-tower-0.1.0.jar
nf-tower-0.1.0.json
nf-tower-0.1.0.zip
```               

### copyPluginLibs

Copies plugin dependencies jar files in the plugin build directory ie. `$PWD/plugins/nf-amazon/build/target/libs`. 
This is only needed when launching the plugin in *development* mode. 

### copyPluginZip

Copies the plugin zip file to the root project build dir ie. `$PWD/build/plugins/`.

### uploadPluginZip

Uploads the plugin zip file to the S3 repository. Options: 

* `source`: the file local path. 
* `target`: the file remote path. 
* `publicAcl`: make the file public accessible (default: `true`).
* `dryRun`: execute the tasks without uploading file (default: `false`).
* `overwrite`: prevent to overwrite a remote file already existing (default: `false`).
* `skipExisting`: do not upload a file already existing (only if the checksum is the same, default: `true`). 

### uploadPluginMeta

Uploads the plugin JSON meta file to the S3 repository. Options: 

* `source`: the file local path. 
* `target`: the file remote path. 
* `publicAcl`: make the file public accessible (default: `true`).
* `dryRun`: execute the tasks without uploading file (default: `false`).
* `overwrite`: prevent to overwrite a remote file already existing (default: `false`).
* `skipExisting`: do not upload a file already existing (only if the checksum is the same, default: `true`). 

### upload

Uploads the plugin both zip and meat files. 

### publishIndex

Upload the plugins index to the repository hosted at [https://github.com/nextflow-io/plugins](https://github.com/nextflow-io/plugins), which makes 
them accessible through the URL [https://raw.githubusercontent.com/nextflow-io/plugins/main/plugins.json](https://raw.githubusercontent.com/nextflow-io/plugins/main/plugins.json). 


## Links 

* https://pf4j.org/
* https://proandroiddev.com/understanding-gradle-the-build-lifecycle-5118c1da613f
