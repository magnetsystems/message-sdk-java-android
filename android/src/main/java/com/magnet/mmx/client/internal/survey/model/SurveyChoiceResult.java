/**
 * File generated by Magnet Magnet Lang Tool on Apr 11, 2016 1:09:57 PM
 * @see {@link http://developer.magnet.com}
 */

package com.magnet.mmx.client.internal.survey.model;

public class SurveyChoiceResult {

  
  
  private String selectedChoiceId;

  
  private Long count;

  public String getSelectedChoiceId() {
    return selectedChoiceId;
  }

  public Long getCount() {
    return count;
  }


  /**
  * Builder for SurveyChoiceResult
  **/
  public static class SurveyChoiceResultBuilder {
    private SurveyChoiceResult toBuild = new SurveyChoiceResult();

    public SurveyChoiceResultBuilder() {
    }

    public SurveyChoiceResult build() {
      return toBuild;
    }

    public SurveyChoiceResultBuilder selectedChoiceId(String value) {
      toBuild.selectedChoiceId = value;
      return this;
    }

    public SurveyChoiceResultBuilder count(Long value) {
      toBuild.count = value;
      return this;
    }
  }
}