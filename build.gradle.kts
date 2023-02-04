plugins {
    `java-gradle-plugin`
    `maven-publish`
    id("org.springframework.boot") version "3.0.2"
    id("io.spring.dependency-management") version "1.1.0"
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

//dependencyManagement {
//    imports {
//        mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
//    }
//}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    annotationProcessor("org.projectlombok:lombok")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

gradlePlugin {
    plugins {
        create("vidr") {
            id = "${project.group}.vidr"
            implementationClass = "${project.group}.vidr.VIDRPlugin"
        }
    }
}

tasks.getByName<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = false
}

tasks.getByName<Jar>("jar") {
    enabled = true
}

springBoot {
    mainClass.set("de.daniel.marlinghaus.vidr.VIDRPlugin")
}

publishing    {
    publications {
        create<MavenPublication>("${project.group}.vidr") {
            groupId = "${project.group}"
            artifactId = "${project.group}.vidr"
            version = "${project.version}"

            artifact(tasks.named("jar"))
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
