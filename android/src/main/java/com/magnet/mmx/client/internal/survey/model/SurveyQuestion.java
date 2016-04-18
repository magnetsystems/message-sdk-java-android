/**
 * File generated by Magnet Magnet Lang Tool on Apr 11, 2016 1:09:57 PM
 * @see {@link http://developer.magnet.com}
 */

package com.magnet.mmx.client.internal.survey.model;

public class SurveyQuestion {

  
  
  private String text;

  
  private String questionId;

  
  private java.util.List<SurveyOption> choices;

  
  private Integer displayOrder;

  
  private SurveyQuestionType type;

  public String getText() {
    return text;
  }

  public String getQuestionId() {
    return questionId;
  }

  public java.util.List<SurveyOption> getChoices() {
    return choices;
  }

  public Integer getDisplayOrder() {
    return displayOrder;
  }

  public SurveyQuestionType getType() {
    return type;
  }


  /**
  * Builder for SurveyQuestion
  **/
  public static class SurveyQuestionBuilder {
    private SurveyQuestion toBuild = new SurveyQuestion();

    public SurveyQuestionBuilder() {
    }

    public SurveyQuestion build() {
      return toBuild;
    }

    public SurveyQuestionBuilder text(String value) {
      toBuild.text = value;
      return this;
    }

    public SurveyQuestionBuilder questionId(String value) {
      toBuild.questionId = value;
      return this;
    }

    public SurveyQuestionBuilder choices(java.util.List<SurveyOption> value) {
      toBuild.choices = value;
      return this;
    }

    public SurveyQuestionBuilder displayOrder(Integer value) {
      toBuild.displayOrder = value;
      return this;
    }

    public SurveyQuestionBuilder type(SurveyQuestionType value) {
      toBuild.type = value;
      return this;
    }
  }
}