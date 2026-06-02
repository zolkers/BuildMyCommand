import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication

if (project.name != "examples" && project.name != "intellij-plugin") {
    plugins.apply("maven-publish")

    extensions.configure<PublishingExtension>("publishing") {
        publications {
            create<MavenPublication>("mavenJava") {
                from(components["java"])
                artifactId = project.path.removePrefix(":").replace(':', '-')

                pom {
                    name.set("BuildMyCommand ${project.path}")
                    description.set("A modular Java command framework module.")
                    url.set("https://github.com/riege/buildmycommand")
                    licenses {
                        license {
                            name.set("MIT License")
                            url.set("https://opensource.org/licenses/MIT")
                        }
                    }
                    developers {
                        developer {
                            id.set("riege")
                            name.set("Riege")
                        }
                    }
                    scm {
                        connection.set("scm:git:https://github.com/riege/buildmycommand.git")
                        developerConnection.set("scm:git:ssh://git@github.com/riege/buildmycommand.git")
                        url.set("https://github.com/riege/buildmycommand")
                    }
                }
            }
        }

        repositories {
            maven {
                name = "buildDirectory"
                url = layout.buildDirectory.dir("repo").get().asFile.toURI()
            }
        }
    }
}
