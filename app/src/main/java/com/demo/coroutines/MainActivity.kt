package com.demo.coroutines

import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.bn1
import kotlinx.android.synthetic.main.activity_main.bn2
import kotlinx.android.synthetic.main.activity_main.bn3
import kotlinx.android.synthetic.main.activity_main.bn4
import kotlinx.android.synthetic.main.activity_main.bn5
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.CoroutineStart.ATOMIC
import kotlinx.coroutines.experimental.CoroutineStart.LAZY
import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext
import kotlin.coroutines.experimental.AbstractCoroutineContextElement
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.coroutines.experimental.startCoroutine
import kotlin.coroutines.experimental.suspendCoroutine

class MainActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    bn1.setOnClickListener {
      test()
    }
    bn2.setOnClickListener {
      joinJob()
    }
    bn3.setOnClickListener {
      testAsync()
    }
    bn4.setOnClickListener {
      testWithContext()
    }
    bn5.setOnClickListener {
      source()
    }
  }

  private fun test() {
    launch(context = UI) {
      val isUIThread = Thread.currentThread() == Looper.getMainLooper().thread
      println("UI::===$isUIThread")
    }
    launch(context = CommonPool) {
      val isUIThread = Thread.currentThread() == Looper.getMainLooper().thread
      println("UI::===$isUIThread")
    }
    launch(context = Unconfined) {
      val isUIThread = Thread.currentThread() == Looper.getMainLooper().thread
      println("UI::===$isUIThread")
    }
  }

  private val job1 by lazy {
    // job1 不会马上启动
    launch(context = Unconfined, start = LAZY) {
      print("I'm job1")
    }
  }

  private fun joinJob() {
    launch {
      println("I'm job2")
      job1.start()
      println("Job end")
    }
  }

  private fun testAsync() {
    val deferred1 = async(CommonPool) {
      "async1"
    }
    async(UI) {
      println("async2")
      println(deferred1.await())
    }
  }

  private fun testWithContext() {
    launch {
      println("launch")
      // 在携程中挂起代码块，并挂起协程直到代码块完成
      withContext(CommonPool, ATOMIC, {
        println("with context")
      })
      println("job end")
    }

  }
  // 自定义一个CoroutineContext
  class ParamContext(val par: String) : AbstractCoroutineContextElement(ParamContext) {
    companion object Key : CoroutineContext.Key<ParamContext>
  }
  // 创建一个Continuation并运行
  private fun createContinuation(param: String, block: suspend () -> Unit) {
    val continuation = object : Continuation<Unit> {
      override val context: CoroutineContext
        get() = ParamContext(param)
      // 运行后调用
      override fun resume(value: Unit) {
        Log.d("TAG", "value:$value param:${context[ParamContext]!!.par}")
      }
      // 运行出错调用
      override fun resumeWithException(exception: Throwable) {
        Log.d("TAG", exception.message)
      }
    }
    block.startCoroutine(continuation)
  }

  private fun source() {
    Log.d("TAG", "before continuation")
    createContinuation("it's param", {
      Log.d("TAG", "before suspend")
      // 支持一个挂起函数
      val result: String = suspendCoroutine { continuation ->
        // 运行耗时函数
        continuation.resume(calcDoSomething())
      }
      Log.d("TAG","after suspend")
      Log.d("TAG", "result:$result")
    })
    Log.d("TAG", "after continuation")
  }

  // 模拟一个耗时函数
  private fun calcDoSomething(): String {
    Thread.sleep(1000)
    Log.d("TAG", "do something")
    return "result"
  }
}
