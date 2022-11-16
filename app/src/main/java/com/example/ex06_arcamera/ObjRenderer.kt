package com.example.ex06_arcamera

import android.content.Context
import android.graphics.BitmapFactory
import android.opengl.GLES30
import android.opengl.GLUtils
import android.opengl.Matrix
import android.util.Log
import de.javagl.obj.Obj
import de.javagl.obj.ObjData
import de.javagl.obj.ObjReader
import de.javagl.obj.ObjUtils
import java.io.IOException
import java.util.*

class ObjRenderer(
	private val mContext: Context?,
	private val mObjName: String,
	private val mTextureName: String
) {
	private val vertexShaderString = """uniform mat4 uMvMatrix;
uniform mat4 uMvpMatrix;
attribute vec4 aPosition;
attribute vec3 aNormal;
attribute vec2 aTexCoord;
varying vec3 vPosition;
varying vec3 vNormal;
varying vec2 vTexCoord;
void main() {
   vPosition = (uMvMatrix * aPosition).xyz;
   vNormal = normalize((uMvMatrix * vec4(aNormal, 0.0)).xyz);
   vTexCoord = aTexCoord;
   gl_Position = uMvpMatrix * vec4(aPosition.xyz, 1.0);
}"""
	private val fragmentShaderString = """precision mediump float;
uniform sampler2D uTexture;
varying vec3 vPosition;
varying vec3 vNormal;
varying vec2 vTexCoord;
void main() {
    gl_FragColor = texture2D(uTexture, vec2(vTexCoord.x, 1.0 - vTexCoord.y));
}"""
	private var mObj: Obj? = null
	private var mProgram = 0
	private var mTextures: IntArray? = null
	private var mVbos: IntArray? = null
	private var mVerticesBaseAddress = 0
	private var mTexCoordsBaseAddress = 0
	private var mNormalsBaseAddress = 0
	private var mIndicesCount = 0
	val mModelMatrix = FloatArray(16)
	private val mViewMatrix = FloatArray(16)
	private val mProjMatrix = FloatArray(16)
	private var mMinPoint: FloatArray? = null
	private var mMaxPoint: FloatArray? = null

	var firstDraw = true

	var moveCnt = 0
	var dist = 1f
	fun moving(){
//		moveCnt++
//		if(moveCnt>=100){
//			dist*=-1
//			moveCnt=0
//		}

		var pos = getPositionXYZ()
		if(pos[0] > 100 || pos[0] < -100){
			dist*=-1
		}

		Matrix.translateM(mModelMatrix,0,dist,0f,0f)

	}

	fun getPositionXYZ(): FloatArray {
		var res = FloatArray(4)
		Matrix.multiplyMV(res, 0, mModelMatrix, 0, floatArrayOf(0f, 0f, 0f, 1f), 0)

		// Log.d("getPositionXYZ 여", Arrays.toString(res))

		return res
	}

	fun init() {
		try {
			val `is` = mContext!!.assets.open(mObjName)
			val bmp = BitmapFactory.decodeStream(mContext.assets.open(mTextureName))
			mObj = ObjReader.read(`is`)
			mObj = ObjUtils.convertToRenderable(mObj)
			mTextures = IntArray(1)
			GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
			GLES30.glGenTextures(1, mTextures, 0)
			GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, mTextures!![0])
			GLES30.glTexParameteri(
				GLES30.GL_TEXTURE_2D,
				GLES30.GL_TEXTURE_MIN_FILTER,
				GLES30.GL_LINEAR_MIPMAP_LINEAR
			)
			GLES30.glTexParameteri(
				GLES30.GL_TEXTURE_2D,
				GLES30.GL_TEXTURE_MAG_FILTER,
				GLES30.GL_LINEAR
			)
			GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bmp, 0)
			GLES30.glGenerateMipmap(GLES30.GL_TEXTURE_2D)
			GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)
			bmp.recycle()
		} catch (e: IOException) {
			Log.e(TAG, e.message!!)
		}
		if (mObj == null || mTextures!![0] == -1) {
			Log.e(TAG, "Failed to init obj - $mObjName, $mTextureName")
		}
		val indices = ObjData.convertToShortBuffer(ObjData.getFaceVertexIndices(mObj, 3))
		val vertices = ObjData.getVertices(mObj)
		val texCoords = ObjData.getTexCoords(mObj, 2)
		val normals = ObjData.getNormals(mObj)
		mVbos = IntArray(2)
		GLES30.glGenBuffers(2, mVbos, 0)
		mVerticesBaseAddress = 0
		mTexCoordsBaseAddress = mVerticesBaseAddress + 4 * vertices.limit()
		mNormalsBaseAddress = mTexCoordsBaseAddress + 4 * texCoords.limit()
		val totalBytes = mNormalsBaseAddress + 4 * normals.limit()
		mIndicesCount = indices.limit()

		// vertexBufferId
		GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mVbos!![0])
		GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, totalBytes, null, GLES30.GL_STATIC_DRAW)
		GLES30.glBufferSubData(
			GLES30.GL_ARRAY_BUFFER,
			mVerticesBaseAddress,
			4 * vertices.limit(),
			vertices
		)
		GLES30.glBufferSubData(
			GLES30.GL_ARRAY_BUFFER,
			mTexCoordsBaseAddress,
			4 * texCoords.limit(),
			texCoords
		)
		GLES30.glBufferSubData(
			GLES30.GL_ARRAY_BUFFER,
			mNormalsBaseAddress,
			4 * normals.limit(),
			normals
		)
		GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)

		// indexBufferId
		GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, mVbos!![1])
		GLES30.glBufferData(
			GLES30.GL_ELEMENT_ARRAY_BUFFER,
			2 * mIndicesCount,
			indices,
			GLES30.GL_STATIC_DRAW
		)
		GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, 0)
		val vShader = GLES30.glCreateShader(GLES30.GL_VERTEX_SHADER)
		GLES30.glShaderSource(vShader, vertexShaderString)
		GLES30.glCompileShader(vShader)
		val compiled = IntArray(1)
		GLES30.glGetShaderiv(vShader, GLES30.GL_COMPILE_STATUS, compiled, 0)
		if (compiled[0] == 0) {
			Log.e(TAG, "Could not compile vertex shader.")
			GLES30.glDeleteShader(vShader)
		}
		val fShader = GLES30.glCreateShader(GLES30.GL_FRAGMENT_SHADER)
		GLES30.glShaderSource(fShader, fragmentShaderString)
		GLES30.glCompileShader(fShader)
		GLES30.glGetShaderiv(fShader, GLES30.GL_COMPILE_STATUS, compiled, 0)
		if (compiled[0] == 0) {
			Log.e(TAG, "Could not compile fragment shader.")
			GLES30.glDeleteShader(fShader)
		}
		mProgram = GLES30.glCreateProgram()
		GLES30.glAttachShader(mProgram, vShader)
		GLES30.glAttachShader(mProgram, fShader)
		GLES30.glLinkProgram(mProgram)
		val linked = IntArray(1)
		GLES30.glGetProgramiv(mProgram, GLES30.GL_LINK_STATUS, linked, 0)
		if (linked[0] == 0) {
			Log.e(TAG, "Could not link program.")
		}
	}

	fun draw() {
		val mvMatrix = FloatArray(16)
		val mvpMatrix = FloatArray(16)
		Matrix.multiplyMM(mvMatrix, 0, mViewMatrix, 0, mModelMatrix, 0)
		Matrix.multiplyMM(mvpMatrix, 0, mProjMatrix, 0, mvMatrix, 0)
		GLES30.glUseProgram(mProgram)
		val mv = GLES30.glGetUniformLocation(mProgram, "uMvMatrix")
		val mvp = GLES30.glGetUniformLocation(mProgram, "uMvpMatrix")
		val position = GLES30.glGetAttribLocation(mProgram, "aPosition")
		val normal = GLES30.glGetAttribLocation(mProgram, "aNormal")
		val texCoord = GLES30.glGetAttribLocation(mProgram, "aTexCoord")
		val texture = GLES30.glGetUniformLocation(mProgram, "uTexture")
		GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
		GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, mTextures!![0])
		GLES30.glUniform1i(texture, 0)
		GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mVbos!![0])
		GLES30.glVertexAttribPointer(position, 3, GLES30.GL_FLOAT, false, 0, mVerticesBaseAddress)
		GLES30.glVertexAttribPointer(normal, 3, GLES30.GL_FLOAT, false, 0, mNormalsBaseAddress)
		GLES30.glVertexAttribPointer(texCoord, 2, GLES30.GL_FLOAT, false, 0, mTexCoordsBaseAddress)
		GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
		GLES30.glUniformMatrix4fv(mv, 1, false, mvMatrix, 0)
		GLES30.glUniformMatrix4fv(mvp, 1, false, mvpMatrix, 0)
		GLES30.glEnableVertexAttribArray(position)
		GLES30.glEnableVertexAttribArray(normal)
		GLES30.glEnableVertexAttribArray(texCoord)
		GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, mVbos!![1])
		GLES30.glDrawElements(GLES30.GL_TRIANGLES, mIndicesCount, GLES30.GL_UNSIGNED_SHORT, 0)
		GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, 0)
		GLES30.glDisableVertexAttribArray(position)
		GLES30.glDisableVertexAttribArray(normal)
		GLES30.glDisableVertexAttribArray(texCoord)
		GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)
	}

	fun setModelMatrix(modelMatrix: FloatArray?) {
		System.arraycopy(modelMatrix, 0, mModelMatrix, 0, 16)
	}

	fun setProjectionMatrix(projMatrix: FloatArray?) {
		System.arraycopy(projMatrix, 0, mProjMatrix, 0, 16)
	}

	fun setViewMatrix(viewMatrix: FloatArray?) {
		System.arraycopy(viewMatrix, 0, mViewMatrix, 0, 16)
	}

	val minPoint: FloatArray
		get() {
			calculateMinMaxPoint()
			val mvMatrix = FloatArray(16)
			val mvpMatrix = FloatArray(16)
			Matrix.multiplyMM(mvMatrix, 0, mViewMatrix, 0, mModelMatrix, 0)
			Matrix.multiplyMM(mvpMatrix, 0, mProjMatrix, 0, mvMatrix, 0)
			val minPoint = FloatArray(4)
			Matrix.multiplyMV(
				minPoint,
				0,
				mModelMatrix,
				0,
				floatArrayOf(mMinPoint!![0], mMinPoint!![1], mMinPoint!![2], 1.0f),
				0
			)
			return minPoint
		}
	val maxPoint: FloatArray
		get() {
			calculateMinMaxPoint()
			val mvMatrix = FloatArray(16)
			val mvpMatrix = FloatArray(16)
			Matrix.multiplyMM(mvMatrix, 0, mViewMatrix, 0, mModelMatrix, 0)
			Matrix.multiplyMM(mvpMatrix, 0, mProjMatrix, 0, mvMatrix, 0)
			val maxPoint = FloatArray(4)
			Matrix.multiplyMV(
				maxPoint,
				0,
				mModelMatrix,
				0,
				floatArrayOf(mMaxPoint!![0], mMaxPoint!![1], mMaxPoint!![2], 1.0f),
				0
			)
			return maxPoint
		}

	fun calculateMinMaxPoint() {
		if (mMinPoint == null || mMaxPoint == null) {
			mMinPoint = FloatArray(3)
			mMaxPoint = FloatArray(3)
			val vertices = ObjData.getVerticesArray(mObj)
			mMinPoint!![0] = vertices[0]
			mMinPoint!![1] = vertices[1]
			mMinPoint!![2] = vertices[2]
			mMaxPoint!![0] = vertices[0]
			mMaxPoint!![1] = vertices[1]
			mMaxPoint!![2] = vertices[2]
			for (i in 1 until mObj!!.numVertices) {
				mMinPoint!![0] = Math.min(mMinPoint!![0], vertices[i * 3])
				mMinPoint!![1] = Math.min(mMinPoint!![1], vertices[i * 3 + 1])
				mMinPoint!![2] = Math.min(mMinPoint!![2], vertices[i * 3 + 2])
				mMaxPoint!![0] = Math.max(mMaxPoint!![0], vertices[i * 3])
				mMaxPoint!![1] = Math.max(mMaxPoint!![1], vertices[i * 3 + 1])
				mMaxPoint!![2] = Math.max(mMaxPoint!![2], vertices[i * 3 + 2])
			}
		}
	}

	companion object {
		private val TAG = ObjRenderer::class.java.simpleName
	}


}