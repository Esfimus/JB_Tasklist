package tasklist

import com.squareup.moshi.*
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.datetime.*
import kotlinx.datetime.TimeZone
import java.io.File
import java.util.*

class Task(private var taskLines: MutableList<String>,
           private var priority: String,
           private var date: String,
           private var time: String) {

    fun getDueTag(): String {
        val dateList = date.split("-").map { it.toInt() }
        val taskDate = LocalDate(dateList[0], dateList[1], dateList[2])
        val currentDate = Clock.System.now().toLocalDateTime(TimeZone.UTC).date
        return if (currentDate.daysUntil(taskDate) == 0) {
            "T"
        } else if (currentDate.daysUntil(taskDate) > 0) {
            "I"
        } else {
            "O"
        }
    }

    fun getTaskLines(): MutableList<String> {
        return taskLines
    }
    fun setTask(taskBlock: MutableList<String>) {
        this.taskLines = taskBlock
    }
    fun getPriority(): String {
        return this.priority
    }
    fun setPriority(priority: String) {
        this.priority = priority
    }
    fun getDate(): String {
        return this.date
    }
    fun setDate(date: String) {
        this.date = date
    }
    fun getTime(): String {
        return this.time
    }
    fun setTime(time: String) {
        this.time = time
    }
}

class TaskList {

    private var taskList = mutableListOf<Task>()

    fun setTaskList(newTaskList: MutableList<Task>) {
        this.taskList = newTaskList
    }

    fun getTaskList(): MutableList<Task> {
        return this.taskList
    }

    fun addTask(task: Task) {
        taskList.add(task)
    }

    /**
     * Nice graphical representation of all tasks
     */
    fun displayTasks() {
        if (taskList.isEmpty()) {
            println("No tasks have been input")
        } else {
            val horLine = "+----+------------+-------+---+---+--------------------------------------------+"
            val header = "| N  |    Date    | Time  | P | D |                   Task                     |"
            val taskWidth = 44
            println(horLine + "\n" + header + "\n" + horLine)
            // iterating through all tasks
            for (i in taskList.indices) {
                // creating multiline withing task column width
                val linesToFitWidth = mutableListOf<String>()
                for (line in taskList[i].getTaskLines()) {
                    // distributing lines
                    if (line.length <= taskWidth) {
                        linesToFitWidth.add(line)
                    } else {
                        for (l in 0..line.length / taskWidth) {
                            if (l == line.length / taskWidth) {
                                val newLine = line.substring(l * taskWidth)
                                if (newLine.isNotEmpty()) linesToFitWidth.add(newLine)
                            } else {
                                val newLine = line.substring(l * taskWidth, (l + 1) * taskWidth)
                                if (newLine.isNotEmpty()) linesToFitWidth.add(newLine)
                            }
                        }
                    }
                }
                // constructing table
                for (j in linesToFitWidth.indices) {
                    val readyLine = if (linesToFitWidth[j].length < taskWidth) {
                        linesToFitWidth[j] + spaceString(taskWidth - linesToFitWidth[j].length)
                    } else {
                        linesToFitWidth[j]
                    }
                    if (j == 0) {
                        // setting tasks' numbers and colors
                        val num = if (i in 0..8) "${i + 1} " else "${i + 1}"
                        val priorityColor = when (taskList[i].getPriority()) {
                            "C" -> "\u001B[101m \u001B[0m"
                            "N" -> "\u001B[102m \u001B[0m"
                            "H" -> "\u001B[103m \u001B[0m"
                            "L" -> "\u001B[104m \u001B[0m"
                            else -> " "
                        }
                        val dueTagColor = when (taskList[i].getDueTag()) {
                            "O" -> "\u001B[101m \u001B[0m"
                            "I" -> "\u001B[102m \u001B[0m"
                            "T" -> "\u001B[103m \u001B[0m"
                            else -> " "
                        }
                        println("| $num | ${taskList[i].getDate()} | ${taskList[i].getTime()} | $priorityColor | $dueTagColor |$readyLine|")
                        } else {
                            println("|    |            |       |   |   |$readyLine|")
                    }
                }
                println(horLine)
            }
        }
    }

    /**
     * Creates a string with fixed number of spaces
     */
    private fun spaceString(spaces: Int): String {
        var string = ""
        for (i in 1..spaces) {
            string += " "
        }
        return string
    }

    /**
     * Simple representation of all tasks
     */
    fun displayTasksSimple() {
        if (taskList.isEmpty()) {
            println("No tasks have been input")
        } else {
            for (i in taskList.indices) {
                val number = if (i in 0..8) "${i + 1} " else "${i + 1}"
                println("$number ${taskList[i].getDate()} ${taskList[i].getTime()} " +
                        "${taskList[i].getPriority()} ${taskList[i].getDueTag()}")
                val taskLines = taskList[i].getTaskLines()
                for (taskLine in taskLines) {
                    println("   $taskLine")
                }
                println()
            }
        }
    }
}

/**
 * Interactive menu for creating and displaying task lists
 */
fun taskApp() {
    val myTaskList = jsonRead()
    do {
        println("Input an action (add, print, edit, delete, end):")
        val userInput = readln().lowercase(Locale.getDefault())
        when (userInput) {
            "add" -> add(myTaskList)
            "print" -> myTaskList.displayTasks()
            "edit" -> edit(myTaskList)
            "delete" -> delete(myTaskList)
            "end" -> {
                jsonSave(myTaskList)
                println("Tasklist exiting!")
            }
            else -> println("The input action is invalid")
        }
    } while (userInput != "end")
}

/**
 * Saves task list to .json file
 */
fun jsonSave(myTaskList: TaskList) {
    if (myTaskList.getTaskList().isNotEmpty()) {
        val jsonFile = File("tasklist.json")
        // standard builder pattern
        val moshi: Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        // creating new parametrized type to extract list of tasks from TaskList object
        val type = Types.newParameterizedType(MutableList::class.java, Task::class.java)
        // addressing the list of tasks with adapter and new type
        val taskListAdapter = moshi.adapter<MutableList<Task>>(type)
        jsonFile.writeText(taskListAdapter.indent(" ").toJson(myTaskList.getTaskList()))
    }
}

/**
 * Reads task list from .json file
 */
fun jsonRead(): TaskList {
    val jsonFile = File("tasklist.json")
    return if (jsonFile.exists()) {
        // standard builder pattern
        val moshi: Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        // creating new parametrized type to write list of tasks to TaskList object
        val type = Types.newParameterizedType(MutableList::class.java, Task::class.java)
        val taskListAdapter = moshi.adapter<MutableList<Task>>(type)
        // reading .json file and creating new task list object
        val newTaskList = taskListAdapter.fromJson(jsonFile.readText())
        val taskList = TaskList()
        if (newTaskList != null) {
            taskList.setTaskList(newTaskList)
        }
        taskList
    } else {
        TaskList()
    }
}

/**
 * Adds new task block (several task lines) to a task list
 */
fun add(myTaskList: TaskList) {
    var priority: String
    var date = ""
    var time = ""
    var taskLine: String

    // adding priority
    do {
        println("Input the task priority (C, H, N, L):")
        var rightPriority = false
        priority = readln().uppercase(Locale.getDefault())
        if (priority == "C" || priority == "H" || priority == "N" || priority == "L") {
            rightPriority = true
        }
    } while (!rightPriority)

    // adding date
    do {
        println("Input the date (yyyy-mm-dd):")
        var rightDate = false
        try {
            val dateArray = readln().split("-").map { it.toInt() }
            val localDate = LocalDate(dateArray[0], dateArray[1], dateArray[2])
            date = "${"%04d".format(localDate.year)}-${"%02d".format(localDate.monthNumber)}-${"%02d".format(localDate.dayOfMonth)}"
            rightDate = true
        } catch(e: Exception) {
            println("The input date is invalid")
        }
    }while(!rightDate)

    // adding time
    do {
        println("Input the time (hh:mm):")
        var rightTime = false
        try {
            val timeArray = readln().split(":").map { it.toInt() }
            val localDateTime = LocalDateTime(2022, 11, 24, timeArray[0], timeArray[1])
            time = "${"%02d".format(localDateTime.hour)}:${"%02d".format(localDateTime.minute)}"
            rightTime = true
        } catch(e: Exception) {
            println("The input time is invalid")
        }
    }while(!rightTime)

    // adding a new task
    println("Input a new task (enter a blank line to end):")
    val singleTaskList = mutableListOf<String>()
    do {
        taskLine = readln()
        val regex = "\\s+".toRegex()
        if (regex.matches(taskLine) || taskLine == "") {
            break
        }
        singleTaskList.add(taskLine)
    } while (true)
    if (singleTaskList.isEmpty()) {
        println("The task is blank")
        return
    }
    val newTask = Task(singleTaskList, priority, date, time)
    myTaskList.addTask(newTask)
}

/**
 * Deletes any task from a task list
 */
fun delete(myTaskList: TaskList) {
    myTaskList.displayTasks()
    if (myTaskList.getTaskList().isEmpty()) {
        return
    } else {
        do {
            // identifying the task number
            println("Input the task number (1-${myTaskList.getTaskList().size}):")
            val inputTaskNumber = readln()
            if (inputTaskNumber.toIntOrNull() != null && inputTaskNumber.toInt() in 1..myTaskList.getTaskList().size) {
                myTaskList.getTaskList().removeAt(inputTaskNumber.toInt() - 1)
                println("The task is deleted")
                break
            } else {
                println("Invalid task number")
            }
        } while (true)
    }
}

/**
 * Edits all four fields of a task from the task list
 */
fun edit(myTaskList: TaskList) {
    myTaskList.displayTasks()
    if (myTaskList.getTaskList().isEmpty()) {
        return
    } else {
        do {
            // identifying the task number
            println("Input the task number (1-${myTaskList.getTaskList().size}):")
            val inputTaskNumber = readln()
            if (inputTaskNumber.toIntOrNull() != null && inputTaskNumber.toInt() in 1..myTaskList.getTaskList().size) {
                do {
                    println("Input a field to edit (priority, date, time, task):")
                    when (readln()) {
                        "priority" -> {
                            // adding priority
                            do {
                                println("Input the task priority (C, H, N, L):")
                                var rightPriority = false
                                val priority = readln().uppercase(Locale.getDefault())
                                if (priority == "C" || priority == "H" || priority == "N" || priority == "L") {
                                    myTaskList.getTaskList()[inputTaskNumber.toInt() - 1].setPriority(priority)
                                    rightPriority = true
                                }
                            } while (!rightPriority)
                            println("The task is changed")
                            break
                        }
                        "date" -> {
                            // adding date
                            do {
                                println("Input the date (yyyy-mm-dd):")
                                var rightDate = false
                                try {
                                    val dateArray = readln().split("-").map { it.toInt() }
                                    val localDate = LocalDate(dateArray[0], dateArray[1], dateArray[2])
                                    val date = "${"%04d".format(localDate.year)}-${"%02d".format(localDate.monthNumber)}-${"%02d".format(localDate.dayOfMonth)}"
                                    myTaskList.getTaskList()[inputTaskNumber.toInt() - 1].setDate(date)
                                    rightDate = true
                                } catch(e: Exception) {
                                    println("The input date is invalid")
                                }
                            }while(!rightDate)
                            println("The task is changed")
                            break
                        }
                        "time" -> {
                            // adding time
                            do {
                                println("Input the time (hh:mm):")
                                var rightTime = false
                                try {
                                    val timeArray = readln().split(":").map { it.toInt() }
                                    val localDateTime = LocalDateTime(2022, 11, 24, timeArray[0], timeArray[1])
                                    val time = "${"%02d".format(localDateTime.hour)}:${"%02d".format(localDateTime.minute)}"
                                    myTaskList.getTaskList()[inputTaskNumber.toInt() - 1].setTime(time)
                                    rightTime = true
                                } catch(e: Exception) {
                                    println("The input time is invalid")
                                }
                            }while(!rightTime)
                            println("The task is changed")
                            break
                        }
                        "task" -> {
                            // adding a new task
                            println("Input a new task (enter a blank line to end):")
                            val singleTaskList = mutableListOf<String>()
                            do {
                                val taskLine = readln()
                                val regex = "\\s+".toRegex()
                                if (regex.matches(taskLine) || taskLine == "") {
                                    break
                                }
                                singleTaskList.add(taskLine)
                            } while (true)
                            if (singleTaskList.isEmpty()) {
                                println("The task is blank")
                                return
                            }
                            myTaskList.getTaskList()[inputTaskNumber.toInt() - 1].setTask(singleTaskList)
                            println("The task is changed")
                            break
                        }
                        else -> println("Invalid field")
                    }
                } while (true)
                break
            } else {
                println("Invalid task number")
            }
        } while (true)
    }
}

fun main() {
    taskApp()
}