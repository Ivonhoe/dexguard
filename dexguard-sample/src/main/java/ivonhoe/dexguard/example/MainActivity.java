package ivonhoe.dexguard.example;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import ivonhoe.dexguard.anotaion.MethodGuard;

public class MainActivity extends AppCompatActivity {

    @Override
    @MethodGuard()
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        HelloWorld helloWorld = new HelloWorld();
        helloWorld.testDexGuard();
    }

    @Override
    @MethodGuard()
    protected void onResume() {
        super.onResume();
    }

    @Override
    @MethodGuard()
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private class HelloWorld {

        @MethodGuard()
        private void testDexGuard() {
            Log.d("HelloWorld", "test dexguard");
        }

        private void testDexGuard(int param) {
            Log.d("HelloWorld", "test dexguard");
        }

        private void testDexGuard(String param) {
            Log.d("HelloWorld", "test dexguard");
        }

        @MethodGuard()
        private void testDexGuard2() {
            Log.d("HelloWorld", "test dexguard2");
        }
    }
}
