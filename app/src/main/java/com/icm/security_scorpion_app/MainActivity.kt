package com.icm.security_scorpion_app

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.findNavController
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
    }

    override fun onStart() {
        super.onStart()

        // 🚀 Configuramos el NavController y el BottomNavigationView
        val navController = findNavController(R.id.fragmentContainerView)
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)

        // Configuramos el listener del BottomNavigationView
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    navController.navigate(R.id.homeFragment)
                    true
                }
                R.id.navigation_cameras -> {
                    navController.navigate(R.id.camerasFragment)
                    true
                }
                R.id.navigation_config -> {
                    navController.navigate(R.id.configFragment)
                    true
                }
                else -> false
            }
        }
    }
}
