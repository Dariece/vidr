plugins {
    `java-gradle-plugin`
    `maven-publish`
}

group = "de.daniel.marlinghaus"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_17
java.targetCompatibility = JavaVersion.VERSION_17

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    //to minimize boilerplate code like constructors/settes/getters and so on
    compileOnly("org.projectlombok:lombok:1.18.26") {
        exclude("org.slf4j:slf4j-api")
    }
    annotationProcessor("org.projectlombok:lombok:1.18.26")


    //to reach vulnerability scanners via http
    implementation("org.apache.httpcomponents.client5:httpclient5:5.2.1") {
        exclude("org.slf4j:slf4j-api")
    }
    //for using multimap
    implementation("org.eclipse.collections:eclipse-collections-api:11.1.0")
    implementation("org.eclipse.collections:eclipse-collections:11.1.0")

    //for creating sbom
//    implementation("commons-io:commons-io:2.11.0")
    implementation("org.cyclonedx:cyclonedx-gradle-plugin:1.7.4") {
        exclude("org.apache.maven:maven-core")
        exclude("org.slf4j:slf4j-api")
    }
    //for serialization and deserialization of json
    implementation("com.fasterxml.jackson.core:jackson-databind:2.14.1")
    //for comparable dependency version
//    implementation("com.fasterxml.jackson.core:jackson-core:2.14.1")
//    implementation(gradleApi())

    //Test the plugin functionalities
    testImplementation(gradleTestKit())
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
    testImplementation("org.assertj:assertj-core:3.24.2")
}

gradlePlugin {
    plugins {
        create("${project.group}.vidr") {
            id = "${project.group}.vidr"
            implementationClass = "${project.group}.vidr.VidrPlugin"
        }
    }
}

tasks.getByName<Jar>("jar") {
    enabled = true
}

publishing {
    publications {
        create<MavenPublication>(project.name) {
            groupId = "${project.group}"
            artifactId = project.name
            version = "${project.version}"

            from(components["java"])
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
