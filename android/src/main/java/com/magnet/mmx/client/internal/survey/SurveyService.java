/**
 * File generated by Magnet Magnet Lang Tool on Apr 11, 2016 1:09:57 PM
 * @see {@link http://developer.magnet.com}
 */
package com.magnet.mmx.client.internal.survey;

import com.magnet.mmx.client.internal.survey.model.*;

import retrofit.http.*;
import retrofit.MagnetCall;

public interface SurveyService {

  /**
   * 
   * GET /com.magnet.server/surveys/survey/{surveyId}/poll/results
   * @param surveyId style:Path optional:false
   * @param callback asynchronous callback
   */
  @GET("com.magnet.server/surveys/survey/{surveyId}/poll/results")
  MagnetCall<SurveyResults> getResults(
     @Path("surveyId") String surveyId,
     retrofit.Callback<SurveyResults> callback
  );

  /**
   * 
   * PUT /com.magnet.server/surveys/answers/{surveyId}
   * @param surveyId style:Path optional:false
   * @param body style:Body optional:false
   * @param callback asynchronous callback
   */
  @PUT("com.magnet.server/surveys/answers/{surveyId}")
  MagnetCall<Void> submitSurveyAnswers(
     @Path("surveyId") String surveyId,
     @Body SurveyAnswerRequest body,
     retrofit.Callback<Void> callback
  );

  /**
   * 
   * GET /com.magnet.server/surveys/answers/{surveyId}
   * @param surveyId style:Path optional:false
   * @param callback asynchronous callback
   */
  @GET("com.magnet.server/surveys/answers/{surveyId}")
  MagnetCall<java.util.List<SurveyResponse>> getSurveyAnswers(
     @Path("surveyId") String surveyId,
     retrofit.Callback<java.util.List<SurveyResponse>> callback
  );

  /**
   * 
   * PUT /com.magnet.server/surveys/survey/{surveyId}
   * @param surveyId style:Path optional:false
   * @param body style:Body optional:false
   * @param callback asynchronous callback
   */
  @PUT("com.magnet.server/surveys/survey/{surveyId}")
  MagnetCall<Survey> updateSurvey(
     @Path("surveyId") String surveyId,
     @Body Survey body,
     retrofit.Callback<Survey> callback
  );

  /**
   * 
   * GET /com.magnet.server/surveys/survey/{surveyId}
   * @param surveyId style:Path optional:false
   * @param callback asynchronous callback
   */
  @GET("com.magnet.server/surveys/survey/{surveyId}")
  MagnetCall<Survey> getSurvey(
     @Path("surveyId") String surveyId,
     retrofit.Callback<Survey> callback
  );

  /**
   * 
   * DELETE /com.magnet.server/surveys/survey/{surveyId}
   * @param surveyId style:Path optional:false
   * @param callback asynchronous callback
   */
  @DELETE("com.magnet.server/surveys/survey/{surveyId}")
  MagnetCall<Void> deleteSurvey(
     @Path("surveyId") String surveyId,
     retrofit.Callback<Void> callback
  );

  /**
   * 
   * POST /com.magnet.server/surveys/survey
   * @param body style:Body optional:false
   * @param callback asynchronous callback
   */
  @POST("com.magnet.server/surveys/survey")
  MagnetCall<Survey> createSurvey(
     @Body Survey body,
     retrofit.Callback<Survey> callback
  );

}
