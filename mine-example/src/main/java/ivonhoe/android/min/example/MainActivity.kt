package ivonhoe.android.min.example

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import ivonhoe.andrid.mine.trace.LongTimeTrace

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }


    fun testMethod() {
        var startTime = LongTimeTrace.methodStart()

        // do test
        Thread.sleep(200);

        LongTimeTrace.methodEnd(true, startTime);
    }
}