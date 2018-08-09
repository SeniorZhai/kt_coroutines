package com.demo.coroutines

import org.junit.Test
import kotlin.coroutines.experimental.AbstractCoroutineContextElement
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.coroutines.experimental.startCoroutine
import kotlin.coroutines.experimental.suspendCoroutine

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {

  /*
  * before coroutine
  * in coroutine. Before suspend.
  * in suspend block.
  * calc md5 for test.zip.after resume.
  * in coroutine. After suspend. result = 1533809382538
  * resume kotlin.Unitafter coroutine
  **/

  @Test
  fun testSuspend() {
    println("before coroutine")
    //启动我们的协程
    asyncCalcMd5("test.zip") {
      println("in coroutine. Before suspend.")
      //暂停我们的线程，并开始执行一段耗时操作
      val result: String = suspendCoroutine { continuation ->
        println("in suspend block.")
        continuation.resume(calcMd5(continuation.context[FilePath]!!.path))
        println("after resume.")
      }
      println("in coroutine. After suspend. result = $result")
    }
    println("after coroutine")
  }

  // CoroutineContext 自定义上下文
  class FilePath(val path: String) : AbstractCoroutineContextElement(FilePath) {
    companion object Key : CoroutineContext.Key<FilePath>
  }

  fun asyncCalcMd5(path: String, block: suspend () -> Unit) {
    val continuation = object : Continuation<Unit> {
      override fun resumeWithException(exception: Throwable) {
        print(exception.localizedMessage)
      }

      override fun resume(value: Unit) {
        print("resume $value")
      }

      override val context: CoroutineContext
        get() = FilePath(path)
    }
    block.startCoroutine(continuation)
  }

  // 耗时函数
  fun calcMd5(path: String): String {
    print("calc md5 for $path.")
    //暂时用这个模拟耗时
    Thread.sleep(1000)
    //假设这就是我们计算得到的 MD5 值
    return System.currentTimeMillis().toString()
  }
}
