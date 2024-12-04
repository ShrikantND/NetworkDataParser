plugins {
    id("java")
}

group = "com.test"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.pcap4j:pcap4j-core:1.8.2")

    // Log4j for logging
    implementation("org.apache.logging.log4j:log4j-api:2.17.1")
    implementation("org.apache.logging.log4j:log4j-core:2.17.1")

//    // For CSV parsing
//    implementation("org.apache.commons:commons-csv:1.9.0")

    // For unit testing
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.8.2")
    testImplementation("org.mockito:mockito-core:3.12.4")

}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}
