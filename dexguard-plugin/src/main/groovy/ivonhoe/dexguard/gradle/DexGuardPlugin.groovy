package ivonhoe.dexguard.gradle;

import ivonhoe.dexguard.gradle.utils.Logger;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

class DexGuardPlugin implements Plugin<Project> {

    void apply(Project project) {
        /**
         * 注册transform接口
         */
        def transform = new DexGuardTransform(project)
        project.android.registerTransform(transform)

        project.afterEvaluate {
            project.android.applicationVariants.each { variant ->

                Logger.w("--------------variant.name.capitalize():" + variant.name.capitalize())
                def junkCodeTask = project.tasks.findByName("transformClassesWithJunkCodeFor${variant.name.capitalize()}")
                def dexTask = project.tasks.findByName("transformClassesWithDexFor${variant.name.capitalize()}")

                if (junkCodeTask) {
                    Set<File> injectTaskInputFiles = junkCodeTask.inputs.files.files
                    Set<File> injectTaskOutputFiles = junkCodeTask.outputs.files.files

                    Logger.w(junkCodeTask.name + "=====>Input")
                    injectTaskInputFiles.each { inputFile ->
                        def path = inputFile.absolutePath
                        Logger.w("\t\t" + path)
                    }

                    Logger.w("${junkCodeTask.name}=====>Output")
                    injectTaskOutputFiles.each { outputFile ->
                        def path = outputFile.absolutePath
                        Logger.w("\t\t" + path)
                    }
                }

                if (dexTask) {
                    Set<File> dexTaskInputFiles = dexTask.inputs.files.files
                    Set<File> dexTaskOutputFiles = dexTask.outputs.files.files

                    Logger.w("${dexTask.name}=====>Input:dexTaskInputFiles")
                    dexTaskInputFiles.each { inputFile ->
                        def path = inputFile.absolutePath
                        Logger.w("\t\t" + path)
                    }

                    Logger.w("${dexTask.name}=====>Output:dexTaskInputFiles")
                    dexTaskOutputFiles.each { outputFile ->
                        def path = outputFile.absolutePath
                        Logger.w("\t\t" + path)
                    }
                }
            }
        }
    }
}