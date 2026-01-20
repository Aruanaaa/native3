import java.time.LocalDateTime
import java.util.UUID


abstract class Person(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    protected val role: Role
) {
    abstract fun getAccessLevel(): AccessLevel

    open fun describe(): String {
        return "$name ($role)"
    }
}

class Student(
    name: String,
    val studentId: String,
    val major: String
) : Person(name = name, role = Role.STUDENT) {

    override fun getAccessLevel(): AccessLevel = AccessLevel.BASIC

    override fun describe(): String {
        return "Student $name, major=$major"
    }
}

class Lecturer(
    name: String,
    val department: String
) : Person(name = name, role = Role.LECTURER) {

    override fun getAccessLevel(): AccessLevel = AccessLevel.ADVANCED

    override fun describe(): String {
        return "Lecturer $name, dept=$department"
    }
}

class Staff(
    name: String,
    val position: String
) : Person(name = name, role = Role.STAFF) {

    override fun getAccessLevel(): AccessLevel = AccessLevel.FULL

    override fun describe(): String {
        return "Staff $name, position=$position"
    }
}



enum class Role {
    STUDENT, LECTURER, STAFF
}

enum class AccessLevel {
    BASIC, ADVANCED, FULL
}



// Composition is used instead of deep inheritance
open class Facility(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    private val requiredAccessLevel: AccessLevel
) {
    fun requiredLevel(): AccessLevel = requiredAccessLevel

    open fun info(): String = "Facility: $name"
}

class Building(name: String) : Facility(name = name, requiredAccessLevel = AccessLevel.BASIC)

class Room(name: String) : Facility(name = name, requiredAccessLevel = AccessLevel.BASIC)

class Laboratory(name: String) : Facility(name = name, requiredAccessLevel = AccessLevel.ADVANCED)



interface AccessPolicy {
    fun canAccess(person: Person, facility: Facility): Boolean
}

// Concrete policy implementation
class DefaultAccessPolicy : AccessPolicy {
    override fun canAccess(person: Person, facility: Facility): Boolean {
        return person.getAccessLevel().ordinal >= facility.requiredLevel().ordinal
    }
}


class AccessManager(
    private val policy: AccessPolicy,
    private val logger: AccessLogger
) {
    private val grantedPermissions = mutableSetOf<Pair<String, String>>()

    fun grantAccess(person: Person, facility: Facility) {
        grantedPermissions.add(person.id to facility.id)
        logger.log("Access granted to ${person.describe()} for ${facility.name}")
    }

    fun revokeAccess(person: Person, facility: Facility) {
        grantedPermissions.remove(person.id to facility.id)
        logger.log("Access revoked from ${person.describe()} for ${facility.name}")
    }

    fun requestAccess(person: Person, facility: Facility): Boolean {
        val allowedByPolicy = policy.canAccess(person, facility)
        val explicitlyGranted = grantedPermissions.contains(person.id to facility.id)

        val result = allowedByPolicy || explicitlyGranted
        logger.log("Access request: ${person.describe()} -> ${facility.name} = $result")
        return result
    }
}


interface AccessLogger {
    fun log(message: String)
}

class ConsoleAccessLogger : AccessLogger {
    override fun log(message: String) {
        println("[${LocalDateTime.now()}] $message")
    }
}


fun main() {
    val logger = ConsoleAccessLogger()
    val policy = DefaultAccessPolicy()
    val accessManager = AccessManager(policy, logger)

    val student = Student(name = "Aruana", studentId = "S123", major = "Software Engineering")
    val lecturer = Lecturer(name = "Dr. Maksat", department = "Computer Science")
    val staff = Staff(name = "Daniyar", position = "Security")

    val building = Building("Main корпус")
    val lab = Laboratory("AI Lab")

    accessManager.requestAccess(student, building)   // true
    accessManager.requestAccess(student, lab)        // false

    accessManager.grantAccess(student, lab)
    accessManager.requestAccess(student, lab)        // true

    accessManager.requestAccess(lecturer, lab)       // true
    accessManager.requestAccess(staff, lab)          // true
}
