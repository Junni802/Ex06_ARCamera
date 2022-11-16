package com.example.ex06_arcamera

import android.content.Context
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.Log
import android.view.View
import android.widget.Button
import com.google.ar.core.Session
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

//GLSurfaceView 를 렌더링하는 클래스
class MyGLRenderer(val mContext: MainActivity):GLSurfaceView.Renderer {

	var viewportChange = false
	var mViewMatrix = FloatArray(16)
	var mProjMatrix = FloatArray(16)
	var modelMatrix = FloatArray(16)

	var mCamera:CameraRenderer

	var andy:ObjRenderer
	var box:ObjRenderer
	var box2:ObjRenderer

	var oo1:ObjRenderer
	var oo2:ObjRenderer
	var oo3:ObjRenderer

	var width = 0
	var height = 0

	init{
		mCamera = CameraRenderer()
		andy = ObjRenderer(mContext, "andy.obj", "andy.png")
		box = ObjRenderer(mContext, "crate.obj", "Crate_Base_Color.png")
		box2 = ObjRenderer(mContext, "crate.obj", "Crate_Base_Color.png")

		oo1 = ObjRenderer(mContext, "bed.obj", "bed.jpg")
		oo2 = ObjRenderer(mContext, "chair.obj", "chair.jpg")
		oo3 = ObjRenderer(mContext, "table.obj", "table.jpg")


	}


	///get()  :: textureID 실행시
	// 함수처럼  if(mCamera.mTextures==null) -1 else mCamera.mTextures!![0] 실행한 결과를 준다
	// 즉 처음에는 null 이어서 -1 이지만 init() 이 된 이후에는  mCamera.mTextures!![0] 이 된다
	//만일  val textureID = if(mCamera.mTextures==null) -1 else mCamera.mTextures!![0]
	// 이렇게 하면 mCamera.mTextures 가 null 이 아니어도 mCamera.mTextures!![0] 을 주지 않아
	// get() 으로 하여 호출때마다 null 인지 확인하여 결과를 주게 해야 한다.


	val textureID:Int
		get() = if(mCamera.mTextures==null) -1 else mCamera.mTextures!![0]

	override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
		GLES30.glEnable(GLES30.GL_DEPTH_TEST)  //3차원 입체감을 제공
		GLES30.glClearColor(1f,0.6f,0.6f,1f)
		Log.d("MyGLRenderer 여","onSurfaceCreated")

		andy.init()
		box.init()
		box2.init()






		oo1.init()
		oo2.init()
		oo3.init()
		mCamera.init()
	}

	//화면크기가 변경시 화면 크기를 가져와 작업
	override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
		this.width = width
		this.height = height

		viewportChange = true

		Log.d("MyGLRenderer 여","onSurfaceChanged")
		GLES30.glViewport(0,0,width,height)

		var ratio = width.toFloat()/height
//        Matrix.frustumM(mProjMatrix,0,
//                -ratio, ratio,
//            -1f,
//            1f,
//            2f,
//            7f
//            )


		Matrix.frustumM(mProjMatrix,0,
			-ratio, ratio,
			-1f,
			1f,
			20f,
			7000f
		)

	}

	override fun onDrawFrame(gl: GL10?) {
		//Log.d("MyGLRenderer 여","onDrawFrame")
		GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

		mContext.preRender()  //그릴때 main의 preRender()를 실행한다.

		GLES30.glDepthMask(false)

		mCamera.draw()

		GLES30.glDepthMask(true)



//        Matrix.setLookAtM(mViewMatrix,0,
//            1f,0.5f,2f,
//            0f,0f,0f,
//            0f,1f,0f
//        )

		Matrix.setLookAtM(mViewMatrix,0,
			2000f,3000f,3000f,
			0f,0f,0f,
			0f,1f,0f
		)


		Matrix.setIdentityM(modelMatrix,0)

		Matrix.translateM(modelMatrix,0,100f,0f,0f)

		andy.setModelMatrix(modelMatrix)
		andy.setViewMatrix(mViewMatrix)
		andy.setProjectionMatrix(mProjMatrix)

		andy.draw()

		if(box.firstDraw) { //맨 처음에만 새 좌표 지정
			var modelMatrix1 = FloatArray(16)
			Matrix.setIdentityM(modelMatrix1, 0)
			box.setModelMatrix(modelMatrix1)
			box.firstDraw = false
		}

		box.setViewMatrix(mViewMatrix)
		box.setProjectionMatrix(mProjMatrix)

		box.draw()

		if(box2.firstDraw) { //맨 처음에만 새 좌표 지정

			var modelMatrix2 = FloatArray(16)
			Matrix.setIdentityM(modelMatrix2, 0)
			box2.setModelMatrix(modelMatrix2)
			box2.firstDraw = false
		}
		box2.setViewMatrix(mViewMatrix)
		box2.setProjectionMatrix(mProjMatrix)

		box2.draw()


		oo1.setModelMatrix(modelMatrix)
		oo1.setViewMatrix(mViewMatrix)
		oo1.setProjectionMatrix(mProjMatrix)

		// oo1.draw()

		oo2.setModelMatrix(modelMatrix)
		oo2.setViewMatrix(mViewMatrix)
		oo2.setProjectionMatrix(mProjMatrix)

		//oo2.draw()

		oo3.setModelMatrix(modelMatrix)
		oo3.setViewMatrix(mViewMatrix)
		oo3.setProjectionMatrix(mProjMatrix)

		//oo3.draw()

		// -- 선생님 식 --
//		var me = box.getPositionXYZ()
//		var you = box2.getPositionXYZ()
//
//		if(me[0]-5 < you[0] + 5 && me[0] + 5 > you[0] - 5 &&
	//	me[1]-5 < you[1] + 5 && me[1] + 5 > you[1] - 5 &&
	//me[2]-5 < you[2] + 5 && me[2] + 5 > you[2] - 5 	){
//			Log.d("충돌 체크", "부딛힘")
//		}


	}
}