package com.mastertechsoftware.tasker

/**
 * Kotlin helper functions
 */
class KotlinTask(var runFunc : ((task: KotlinTask) -> Any?)) : DefaultTask<Any>() {

    override fun run(): Any? {
        return runFunc(this)
    }
}

class KotlinCondition(val condFun : () -> Boolean) : Condition {
    override fun shouldExecute(): Boolean {
        return condFun()
    }

}
/**
 * Extension to pass in a function instead of an object
 */
fun Tasker.addTask(runFunc: (task: KotlinTask) -> Any?) : Tasker {
    this.addTask(KotlinTask(runFunc))
    return this
}


fun Tasker.addUITask(runFunc: (task: KotlinTask) -> Any?) : Tasker {
    this.addUITask(KotlinTask(runFunc))
    return this
}

fun Tasker.withCondition(condFun: () -> Boolean) : Tasker {
    this.withCondition(KotlinCondition(condFun))
    return this
}