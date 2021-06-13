package ivonhoe.gradle.dexguard;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        HelloWorld helloWorld = new HelloWorld();
        helloWorld.testDexGuard();
    }

    private class HelloWorld {

        private void testDexGuard() {
            Log.d("HelloWorld", "test dexguard");
        }
    }
}
