import groovy.json.JsonSlurper

def projects = new JsonSlurper().parseText(readFileFromWorkspace("src/main/groovy/project.json"))


def projectScm = { owner, project ->
    delegate = owner
    scm {
        git {
            remote {
                url project.scm
            }
            branch "master"
            createTag false
        }
    }
}

projects.each { project ->

    //Define projects name
    def compileProjectName = "${project.name} - Compile"
    def testProjectName = "${project.name} - Test"
    def packageProjectName = "${project.name} - Package"

    //Compile Job
    mavenJob(compileProjectName) {
        projectScm(delegate, project)
        goals "compile"
        publishers {
            downstream testProjectName
        }
    }

    //Test Job
    mavenJob(testProjectName) {
        projectScm(delegate, project)
        goals "test"
        publishers {
            downstream packageProjectName
        }
    }

    //Package Job
    freeStyleJob(packageProjectName) {
        projectScm(delegate, project)
        steps {
            maven {
                goals "package"
                mavenInstallation "Maven 3.2.2"
            }
            shell "cp submodule/target/*.jar /dev/null"
        }
    }

    //Create the Pipeline view
    buildPipelineView("${project.name}") {
        selectedJob compileProjectName
        displayedBuilds 3
        showPipelineParameters true
        showPipelineParametersInHeaders true
        showPipelineDefinitionHeader true

    }
}