package com.demo.coroutines

import android.os.Bundle
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.bn1
import kotlinx.android.synthetic.main.activity_main.bn2
import kotlinx.android.synthetic.main.activity_main.bn3
import kotlinx.android.synthetic.main.activity_main.bn4
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.CoroutineStart.ATOMIC
import kotlinx.coroutines.experimental.CoroutineStart.LAZY
import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext

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
}
