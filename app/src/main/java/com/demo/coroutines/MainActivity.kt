package com.demo.coroutines

import android.os.Bundle
import android.os.Looper
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
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.ForkJoinWorkerThread
import kotlin.coroutines.experimental.AbstractCoroutineContextElement
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.ContinuationInterceptor
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
      log("${Thread.currentThread().name} UI::===$isUIThread")
    }
    launch(context = CommonPool) {
      val isUIThread = Thread.currentThread() == Looper.getMainLooper().thread
      log("${Thread.currentThread().name} UI::===$isUIThread")
    }
    launch(context = Unconfined) {
      val isUIThread = Thread.currentThread() == Looper.getMainLooper().thread
      log("${Thread.currentThread().name} UI::===$isUIThread")
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
      log("${Thread.currentThread().name} I'm job2")
      job1.start()
      log("${Thread.currentThread().name} Job end")
    }
  }

  private fun testAsync() {
    val deferred1 = async(CommonPool) {
      "async1"
    }
    async(UI) {
      log("${Thread.currentThread().name} async2")
      log(deferred1.await())
    }
  }

  private fun testWithContext() {
    launch {
      log("${Thread.currentThread().name} launch")
      // 在携程中挂起代码块，并挂起协程直到代码块完成
      withContext(CommonPool, ATOMIC, {
        log("${Thread.currentThread().name} with context")
      })
      log("${Thread.currentThread().name} job end")
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
        // 上下文通过+组合
        get() = ParamContext(param) + MyCommonPool

      // 运行后调用
      override fun resume(value: Unit) {
        log("${Thread.currentThread().name} value:$value param:${context[ParamContext]!!.par}")
      }

      // 运行出错调用
      override fun resumeWithException(exception: Throwable) {
        log(exception.message)
      }
    }

    block.startCoroutine(continuation)

  }

  private fun source() {
    log("${Thread.currentThread().name} before continuation")
    createContinuation("it's param", {
      log("${Thread.currentThread().name} before suspend")
      // 支持一个挂起函数
      val result: String = suspendCoroutine { continuation ->
        // 异步运行耗时函数
        continuation.resume(calcDoSomething())
      }
      log("${Thread.currentThread().name} after suspend")
      log("${Thread.currentThread().name} result:$result")
    })
    log("${Thread.currentThread().name} after continuation")
  }

  // 模拟一个耗时函数
  private fun calcDoSomething(): String {
    Thread.sleep(1000)
    log("${Thread.currentThread().name} do something")
    return "result"
  }

  open class Pool(private val pool: ForkJoinPool) : AbstractCoroutineContextElement(
      ContinuationInterceptor), ContinuationInterceptor {
    // 拦截Continuation
    override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T> =
        PoolContinuation(pool, continuation.context.fold(continuation) { cont, element ->
          if (element != this@Pool && element is ContinuationInterceptor) {
            element.interceptContinuation(cont)
          } else {
            cont
          }
        })
  }

  // 自定义一个Continuation
  private class PoolContinuation<T>(val pool: ForkJoinPool,
      val continuation: Continuation<T>) : Continuation<T> by continuation {
    override fun resume(value: T) {
      if (isPoolThread()) continuation.resume(value)
      else pool.execute { continuation.resume(value) }
    }

    override fun resumeWithException(exception: Throwable) {
      if (isPoolThread()) continuation.resumeWithException(exception)
      else continuation.resumeWithException(exception)
    }

    private fun isPoolThread(): Boolean = (Thread.currentThread() as? ForkJoinWorkerThread)?.pool == pool
  }

  object MyCommonPool : Pool(ForkJoinPool.commonPool())

}
