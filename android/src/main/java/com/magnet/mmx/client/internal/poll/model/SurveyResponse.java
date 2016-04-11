/**
 * File generated by Magnet Magnet Lang Tool on Apr 11, 2016 1:09:57 PM
 * @see {@link http://developer.magnet.com}
 */

package com.magnet.mmx.client.internal.poll.model;

public class SurveyResponse {

  
  
  private java.util.Date completedOn;

  
  private String responseId;

  
  private java.util.Date startedOn;

  
  private String userId;

  
  private java.util.List<SurveyAnswer> answers;

  
  private SurveyDefinition survey;

  public java.util.Date getCompletedOn() {
    return completedOn;
  }

  public String getResponseId() {
    return responseId;
  }

  public java.util.Date getStartedOn() {
    return startedOn;
  }

  public String getUserId() {
    return userId;
  }

  public java.util.List<SurveyAnswer> getAnswers() {
    return answers;
  }

  public SurveyDefinition getSurvey() {
    return survey;
  }


  /**
  * Builder for SurveyResponse
  **/
  public static class SurveyResponseBuilder {
    private SurveyResponse toBuild = new SurveyResponse();

    public SurveyResponseBuilder() {
    }

    public SurveyResponse build() {
      return toBuild;
    }

    public SurveyResponseBuilder completedOn(java.util.Date value) {
      toBuild.completedOn = value;
      return this;
    }

    public SurveyResponseBuilder responseId(String value) {
      toBuild.responseId = value;
      return this;
    }

    public SurveyResponseBuilder startedOn(java.util.Date value) {
      toBuild.startedOn = value;
      return this;
    }

    public SurveyResponseBuilder userId(String value) {
      toBuild.userId = value;
      return this;
    }

    public SurveyResponseBuilder answers(java.util.List<SurveyAnswer> value) {
      toBuild.answers = value;
      return this;
    }

    public SurveyResponseBuilder survey(SurveyDefinition value) {
      toBuild.survey = value;
      return this;
    }
  }
}
