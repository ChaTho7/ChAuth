package com.chatho.chauth.view

import android.animation.Animator
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import com.chatho.chauth.R
import com.chatho.chauth.databinding.ActivitySplashBinding

class SplashActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.animationView.addAnimatorListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(p0: Animator) {
                val fadeInAnimation = AnimationUtils.loadAnimation(this@SplashActivity, R.anim.fade)
                binding.appName.startAnimation(fadeInAnimation)
                binding.appName.visibility = View.VISIBLE
            }

            override fun onAnimationEnd(p0: Animator) {
                val intent = Intent(this@SplashActivity, MainActivity::class.java)
                startActivity(intent)
                finish()
            }

            override fun onAnimationCancel(p0: Animator) {}

            override fun onAnimationRepeat(p0: Animator) {}
        })
    }

    override fun onResume() {
        super.onResume()

        binding.animationView.playAnimation()
    }
}