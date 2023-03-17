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
    compileOnly("org.projectlombok:lombok:1.18.26")
    annotationProcessor("org.projectlombok:lombok:1.18.26")

    implementation("org.apache.httpcomponents.client5:httpclient5:5.2.1"){
        exclude("org.slf4j:slf4j-api")
    }
    implementation("org.cyclonedx:cyclonedx-gradle-plugin:1.7.4"){
        exclude("org.apache.maven:maven-core")
        exclude("org.slf4j:slf4j-api")
    }
    implementation(gradleApi())

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

publishing    {
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
