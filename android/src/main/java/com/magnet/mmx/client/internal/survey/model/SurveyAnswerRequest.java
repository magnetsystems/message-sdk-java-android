/**
 * File generated by Magnet Magnet Lang Tool on Apr 11, 2016 1:36:25 PM
 * @see {@link http://developer.magnet.com}
 */

package com.magnet.mmx.client.internal.survey.model;

public class SurveyAnswerRequest {

  
  
  private java.util.Date completedOn;

  
  private java.util.Date startedOn;

  
  private java.util.List<SurveyAnswer> answers;

  public java.util.Date getCompletedOn() {
    return completedOn;
  }

  public java.util.Date getStartedOn() {
    return startedOn;
  }

  public java.util.List<SurveyAnswer> getAnswers() {
    return answers;
  }


  /**
  * Builder for SurveyAnswerRequest
  **/
  public static class SurveyAnswerRequestBuilder {
    private SurveyAnswerRequest toBuild = new SurveyAnswerRequest();

    public SurveyAnswerRequestBuilder() {
    }

    public SurveyAnswerRequest build() {
      return toBuild;
    }

    public SurveyAnswerRequestBuilder completedOn(java.util.Date value) {
      toBuild.completedOn = value;
      return this;
    }

    public SurveyAnswerRequestBuilder startedOn(java.util.Date value) {
      toBuild.startedOn = value;
      return this;
    }

    public SurveyAnswerRequestBuilder answers(java.util.List<SurveyAnswer> value) {
      toBuild.answers = value;
      return this;
    }
  }
}
