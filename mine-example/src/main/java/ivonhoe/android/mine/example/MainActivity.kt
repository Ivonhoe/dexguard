package ivonhoe.android.mine.example

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import ivonhoe.android.mine.trace.LongTimeTrace

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        testMethod1()
    }

    fun testMethod() {
        var startTime = LongTimeTrace.methodStart()

        // do test
        Thread.sleep(200);

        LongTimeTrace.methodEnd(true, startTime);
    }

    fun testMethod1() {
        var test1 = Test1();

        test1.testMethod1()
    }
}