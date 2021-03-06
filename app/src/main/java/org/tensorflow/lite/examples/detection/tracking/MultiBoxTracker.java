/* Copyright 2019 The TensorFlow Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================*/

package org.tensorflow.lite.examples.detection.tracking;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Join;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.speech.tts.TextToSpeech;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.util.TypedValue;

import org.tensorflow.lite.examples.detection.CameraActivity;
import org.tensorflow.lite.examples.detection.env.BorderedText;
import org.tensorflow.lite.examples.detection.env.ImageUtils;
import org.tensorflow.lite.examples.detection.env.Logger;
import org.tensorflow.lite.examples.detection.settingActivity;
import org.tensorflow.lite.examples.detection.tflite.Detector.Recognition;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/** A tracker that handles non-max suppression and matches existing objects to new detections. */
public class MultiBoxTracker {
  private static final float TEXT_SIZE_DIP = 18;
  private static final float MIN_SIZE = 16.0f;
  private static final int[] COLORS = {
    Color.BLUE,
    Color.RED,
    Color.GREEN,
    Color.YELLOW,
    Color.CYAN,
    Color.MAGENTA,
    Color.WHITE,
    Color.parseColor("#55FF55"),
    Color.parseColor("#FFA500"),
    Color.parseColor("#FF8888"),
    Color.parseColor("#AAAAFF"),
    Color.parseColor("#FFFFAA"),
    Color.parseColor("#55AAAA"),
    Color.parseColor("#AA33AA"),
    Color.parseColor("#0D0068")
  };
  final List<Pair<Float, RectF>> screenRects = new LinkedList<Pair<Float, RectF>>();
  private final Logger logger = new Logger();
  private final Queue<Integer> availableColors = new LinkedList<Integer>();
  private final List<TrackedRecognition> trackedObjects = new LinkedList<TrackedRecognition>();
  private final Paint boxPaint = new Paint();
  private final float textSizePx;
  private final BorderedText borderedText;
  private Matrix frameToCanvasMatrix;
  private int frameWidth;
  private int frameHeight;
  private int sensorOrientation;

  public MultiBoxTracker(final Context context) {
    for (final int color : COLORS) {
      availableColors.add(color);
    }

    boxPaint.setColor(Color.RED);
    boxPaint.setStyle(Style.STROKE);
    boxPaint.setStrokeWidth(10.0f);
    boxPaint.setStrokeCap(Cap.ROUND);
    boxPaint.setStrokeJoin(Join.ROUND);
    boxPaint.setStrokeMiter(100);

    textSizePx =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, context.getResources().getDisplayMetrics());
    borderedText = new BorderedText(textSizePx);
  }

  public synchronized void setFrameConfiguration(
      final int width, final int height, final int sensorOrientation) {
    frameWidth = width;
    frameHeight = height;
    this.sensorOrientation = sensorOrientation;
  }

  public synchronized void drawDebug(final Canvas canvas) {
    final Paint textPaint = new Paint();
    textPaint.setColor(Color.WHITE);
    textPaint.setTextSize(60.0f);

    final Paint boxPaint = new Paint();
    boxPaint.setColor(Color.RED);
    boxPaint.setAlpha(200);
    boxPaint.setStyle(Style.STROKE);

    for (final Pair<Float, RectF> detection : screenRects) {
      final RectF rect = detection.second;
      canvas.drawRect(rect, boxPaint);
      canvas.drawText("" + detection.first, rect.left, rect.top, textPaint);
      borderedText.drawText(canvas, rect.centerX(), rect.centerY(), "" + detection.first);
    }
  }

  public synchronized void trackResults(final List<Recognition> results, final long timestamp) {
    logger.i("Processing %d results from %d", results.size(), timestamp);
    processResults(results);
  }

  private Matrix getFrameToCanvasMatrix() {
    return frameToCanvasMatrix;
  }

  public synchronized void draw(final Canvas canvas) {
    final boolean rotated = sensorOrientation % 180 == 90;
    final float multiplier =
        Math.min(
            canvas.getHeight() / (float) (rotated ? frameWidth : frameHeight),
            canvas.getWidth() / (float) (rotated ? frameHeight : frameWidth));
    frameToCanvasMatrix =
        ImageUtils.getTransformationMatrix(
            frameWidth,
            frameHeight,
            (int) (multiplier * (rotated ? frameHeight : frameWidth)),
            (int) (multiplier * (rotated ? frameWidth : frameHeight)),
            sensorOrientation,
            false);
    for (final TrackedRecognition recognition : trackedObjects) {
      final RectF trackedPos = new RectF(recognition.location);

      getFrameToCanvasMatrix().mapRect(trackedPos);
      boxPaint.setColor(recognition.color);

      float cornerSize = Math.min(trackedPos.width(), trackedPos.height()) / 8.0f;
      canvas.drawRoundRect(trackedPos, cornerSize, cornerSize, boxPaint);

      final String labelString =
          !TextUtils.isEmpty(recognition.title)
              ? String.format("%s %.2f", recognition.title, (100 * recognition.detectionConfidence))
              : String.format("%.2f", (100 * recognition.detectionConfidence));
      //            borderedText.drawText(canvas, trackedPos.left + cornerSize, trackedPos.top,
      // labelString);
      borderedText.drawText(
          canvas, trackedPos.left + cornerSize, trackedPos.top, labelString + "%", boxPaint);
    }
  }

  private void processResults(final List<Recognition> results) {
    final List<Pair<Float, Recognition>> rectsToTrack = new LinkedList<Pair<Float, Recognition>>();

    screenRects.clear();
    final Matrix rgbFrameToScreen = new Matrix(getFrameToCanvasMatrix());

    for (final Recognition result : results) {
        if(result.getTitle().equals("pedestrians")){
          if(!settingActivity.detectPedestrians)
          {
            continue;
          }
        }
        if(result.getTitle().equals("carahead")){
          if(!settingActivity.detectCarAhead)
          {
            continue;
          }
        }

        if(result.getTitle().equals("cyclists")){
          if(!settingActivity.detectCyclists)
          {
            continue;
          }
        }
        if(result.getTitle().equals("pothole")){
          if(!settingActivity.detectPothole)
          {
            continue;
          }
        }

        if(result.getTitle().equals("stop")){
          if(!settingActivity.detectStop)
          {
            continue;
          }
        }

      if(result.getTitle().equals("Incomingcar")){
        if(!settingActivity.detectIncomingCar)
        {
          continue;
        }
      }


      if (result.getLocation() == null) {
        continue;
      }
      final RectF detectionFrameRect = new RectF(result.getLocation());

      final RectF detectionScreenRect = new RectF();
      rgbFrameToScreen.mapRect(detectionScreenRect, detectionFrameRect);

      logger.v(
          "Result! Frame: " + result.getLocation() + " mapped to screen:" + detectionScreenRect);

      screenRects.add(new Pair<Float, RectF>(result.getConfidence(), detectionScreenRect));

      if (detectionFrameRect.width() < MIN_SIZE || detectionFrameRect.height() < MIN_SIZE) {
        logger.w("Degenerate rectangle! " + detectionFrameRect);
        continue;
      }

      if(CameraActivity.getMuted() == true){
        CameraActivity.textToSpeech.stop();
      }
      if(CameraActivity.getMuted() == false){
        speakOut(result.getTitle(),result.getConfidence());
      }

      rectsToTrack.add(new Pair<Float, Recognition>(result.getConfidence(), result));
    }

    trackedObjects.clear();
    if (rectsToTrack.isEmpty()) {
      logger.v("Nothing to track, aborting.");
      timerPedestrians = timerPedestrians - timerClass;
      timerCarAhead = timerCarAhead - timerClass;
      timerCyclists = timerCyclists - timerClass;
      timerPothole = timerPothole - timerClass;
      timerStop = timerStop - timerClass;
      return;
    }

    for (final Pair<Float, Recognition> potential : rectsToTrack) {
      final TrackedRecognition trackedRecognition = new TrackedRecognition();
      trackedRecognition.detectionConfidence = potential.first;
      trackedRecognition.location = new RectF(potential.second.getLocation());
      trackedRecognition.title = potential.second.getTitle();
      trackedRecognition.color = COLORS[trackedObjects.size()];
      trackedObjects.add(trackedRecognition);

      if (trackedObjects.size() >= COLORS.length) {
        break;
      }
    }
  }

  public static void setTimer(long timer){
    timerClass = (int) timer;
  }

  public static int timerClass;
  private int timerPedestrians = 0;
  private int timerCarAhead = 0;
  private int timerCyclists = 0;
  private int timerIncomingCar = 0;
  private int timerPothole =0;
  private int timerStop = 0;
  private void speakOut(String word, float score) {
    int amStreamMusicMaxVol = CameraActivity.am.getStreamMaxVolume(CameraActivity.am.STREAM_MUSIC);

    if (settingActivity.volumeCheckbox) {
      float volume = score * 10;
      int ScoreTotal = (int) volume + settingActivity.volumeLevel;
      Log.d("ediedit", "score : " + ScoreTotal);
      CameraActivity.am.setStreamVolume(CameraActivity.am.STREAM_MUSIC, ScoreTotal, 0);
    }

    if(settingActivity.timerWork){
      score = score * 100;
      timerPedestrians = timerPedestrians - timerClass;
      timerCarAhead = timerCarAhead - timerClass;
      timerCyclists = timerCyclists - timerClass;
      timerPothole = timerPothole - timerClass;
      timerStop = timerStop - timerClass;
      timerIncomingCar = timerIncomingCar - timerClass;

      if(word.equals("pedestrians")) {
        if (timerPedestrians <= 0) {
          CameraActivity.textToSpeech.speak(word, TextToSpeech.QUEUE_ADD, null, null);
          timerPedestrians = 4000;
        }
        else{
          timerPedestrians = timerPedestrians - (int)score;
        }
      }

      if(word.equals("carahead")) {
        if (timerCarAhead <= 0) {
          CameraActivity.textToSpeech.speak(word, TextToSpeech.QUEUE_ADD, null, null);
          timerCarAhead = 4000;
        }
        else{
          timerCarAhead = timerCarAhead - (int)score;
        }
      }

      if(word.equals("cyclists")) {
        if (timerCyclists <= 0) {
          CameraActivity.textToSpeech.speak(word, TextToSpeech.QUEUE_ADD, null, null);
          timerCyclists = 4000;
        }
        else{
          timerCyclists = timerCyclists - (int)score;
        }
      }

      if(word.equals("pothole")) {
        if (timerPothole <= 0) {
          CameraActivity.textToSpeech.speak(word, TextToSpeech.QUEUE_ADD, null, null);
          timerPothole = 4000;
        }
        else{
          timerPothole = timerPothole - (int)score;
        }
      }

      if(word.equals("incomingcar")) {
        if (timerIncomingCar <= 0) {
          CameraActivity.textToSpeech.speak(word, TextToSpeech.QUEUE_ADD, null, null);
          timerIncomingCar = 4000;
        }
        else{
          timerIncomingCar = timerIncomingCar - (int)score;
        }
      }

      if(word.equals("stop")) {
        if (timerStop <= 0) {
          CameraActivity.textToSpeech.speak(word, TextToSpeech.QUEUE_ADD, null, null);
          timerStop = 4000;
        }
        else{
          timerStop = timerStop - (int)score;
        }
      }
    }

    if(!settingActivity.timerWork) {
      if (!CameraActivity.textToSpeech.isSpeaking()) {
        CameraActivity.textToSpeech.speak(word, TextToSpeech.QUEUE_FLUSH, null, null);
      }
    }
  }

  private void vibratorNow(int time){
    CameraActivity.vibrator.vibrate(time);
  }
  private static class TrackedRecognition {
    RectF location;
    float detectionConfidence;
    int color;
    String title;
  }
}
