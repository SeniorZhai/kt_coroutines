package com.demo.coroutines

import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun testAwait() {
    runBlocking {
      val s = async { doSomething() }
      print("I will got thw result ${s.await()}") // 因为await会挂起 所以要泡在runBlocking里
    }
  }

  suspend fun doSomething(): String {
    delay(1000)
    return "Result"
  }

  @Test
  fun testChannel() {
    runBlocking {
      val channel = Channel<Int>()
      launch {
        for (x in 0..5) channel.send(x)
        channel.close()
      }

      for (x in 0..5) {
        println(channel.receive())
      }
    }
  }
}