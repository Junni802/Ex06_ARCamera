package com.example.ex06_arcamera

import android.Manifest
import android.hardware.display.DisplayManager
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.example.ex06_arcamera.databinding.ActivityMainBinding
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Frame
import com.google.ar.core.Session
import java.util.*

class MainActivity : AppCompatActivity() {

	var kang = false
	var mSession: Session? = null
	var myGLRenderer:MyGLRenderer? = null
	var myGLView: GLSurfaceView? = null

	//                          가상환경에서 카메라 위치 정보             가상환경에서 카메라가 보고 있는 이미지 (영상)
//                                                                              //실제 화면
	// 실제 카메라 -> mSession -> 가상 카메라 -> MyGLRenderer(onDrawFrame ) ->  CameraRenderer

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)

		requestPermission()

		myGLView = findViewById(R.id.myGLView)

		myGLView!!.setEGLContextClientVersion(3)
		//일시중지시 EGLContext 유지여부
		myGLView!!.preserveEGLContextOnPause=true


		myGLRenderer = MyGLRenderer(this)
		//어떻게 그릴 것인가
		myGLView!!.setRenderer(myGLRenderer)

		//화면 렌더링을 언제 할 것인가 = 렌더러 반복호출하여 장면을 다시 그린다.
		myGLView!!.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY

		//화면 변화 감지
		val displayManager = getSystemService(DISPLAY_SERVICE) as DisplayManager

		displayManager.registerDisplayListener(
			object:DisplayManager.DisplayListener{
				override fun onDisplayAdded(displayId: Int) {

				}

				override fun onDisplayRemoved(displayId: Int) {

				}

				override fun onDisplayChanged(displayId: Int) {
					synchronized(this){
						//화면 변경 와
						myGLRenderer!!.viewportChange = true
					}
				}

			}, null
		)
	}

	fun requestPermission(){
		ActivityCompat.requestPermissions(
			this,
			arrayOf(Manifest.permission.CAMERA),
			1234
		)
	}

	override fun onResume() {
		super.onResume()

		if(ArCoreApk.getInstance().requestInstall(this,true) ==
			ArCoreApk.InstallStatus.INSTALLED  ){
			mSession = Session(this)

			mSession!!.resume()

			Log.d("mSession 여","${mSession}")
		}
		myGLView!!.onResume()
	}

	override fun onPause() {
		super.onPause()

		mSession!!.pause()
		myGLView!!.onPause()
	}

	fun preRender() {
		// Log.d("preRender 여","gogo")

		//화면이 변환되었다면
		if(myGLRenderer!!.viewportChange){

			//회전상태 확인
			val display = windowManager.defaultDisplay

			//세션의 화면 정보 갱신
			//myGLRenderer!!.updateSession(mSession!!, display.rotation)
			mSession!!.setDisplayGeometry(display.rotation, myGLRenderer!!.width, myGLRenderer!!.height)
			//화면 변환 해제
			myGLRenderer!!.viewportChange = false

		}

		//이미 실제 카메라를 세션에서 적용
		// 렌더러에서 사용하도록 지정 --> CameraRenderer로 사용하도록 ID 설정
		mSession!!.setCameraTextureName(myGLRenderer!!.textureID)

		var frame: Frame? = null

		try {
			frame = mSession!!.update()
		}catch (e:Exception){

		}

		if(frame!=null) {  //frame이 null 이 되는 경우가 있어서 null이 아닐때만 실행
			myGLRenderer!!.mCamera.transformDisplayGeometry(frame!!)
		}



		myGLRenderer!!.box.moving()


		if(tt) {
			var pos = myGLRenderer!!.box.getPositionXYZ()
			var modelMatrix2 = myGLRenderer!!.box2.mModelMatrix

			Matrix.translateM(modelMatrix2,0,moveX,moveY,moveZ)
			Matrix.scaleM(modelMatrix2,0,sc,sc,sc)


			fun getPositionXYZ(): FloatArray {
				var res = FloatArray(4)
				Matrix.multiplyMV(res, 0, modelMatrix2, 0, floatArrayOf(0f, 0f, 0f, 1f), 0)

				// Log.d("getPositionXYZ 여", Arrays.toString(res))

				return res
			}

			var pos1 = getPositionXYZ()

			if(pos[0] + 30 > pos1[0] - 30 && pos[0] - 30 < pos1[0] + 30){
				Log.d("충돌 여", "충돌했습니다")
				kang = true
				Log.d("kang 여", "${kang}")
			}

			if(rotateX){
				Matrix.rotateM(modelMatrix2,0,rr,-1f,0f,0f)
			}
			if(rotateY){
				Matrix.rotateM(modelMatrix2,0,rr,0f,-1f,0f)
			}
			if(rotateZ){
				Matrix.rotateM(modelMatrix2,0,rr,0f,0f, -1f)
			}

			tt = false
		}


	}


	var moveX = 0f
	var moveY = 0f
	var moveZ = 0f

	var rr = 5f
	var rotateX = false
	var rotateY = false
	var rotateZ = false

	var tt = false


	var sc = 1f

	fun eventInit(){
		moveX = 0f
		moveY = 0f
		moveZ = 0f
		sc = 1f
		rotateX = false
		rotateY = false
		rotateZ = false
	}

	fun moveGo(v: View){
		var btn = v as Button
//		Log.d("moveGo 여", "${btn.text}")
		eventInit()
		when(btn.text.toString()){
			"←" ->{
				moveX = -10f
			}
			"→" ->{
				moveX = 10f
			}
			"↑" ->{
				moveY = 10f
			}
			"↓" ->{
				moveY = -10f
			}
			"↗" ->{
				moveZ = -10f
			}
			"↙" ->{
				moveZ = 10f
			}
		}

		tt = true
	}

	fun rotateGo(v: View){
		var btn = v as Button
//		Log.d("rotateGo 여", "${btn.text}")
		eventInit()
		when(btn.text.toString()){
			"←" ->{
				rotateX = true
				rr = -5f
			}
			"→" ->{
				rotateX = true
				rr = 5f
			}
			"↑" ->{
				rotateY = true
				rr = -5f
			}
			"↓" ->{
				rotateY = true
				rr = 5f
			}
			"↗" ->{
				rotateZ = true
				rr = -5f
			}
			"↙" ->{
				rotateZ = true
				rr = 5f
			}
		}
		tt = true
	}


	fun scaleGo(v: View){
		var btn = v as Button
//		Log.d("scaleGo 여", "${btn.text}")
		eventInit()
		when(btn.text.toString()){

			"↑" ->{
				sc = 1.2f
			}
			"↓" ->{
				sc = 0.8f
			}
		}
		tt = true
	}

}