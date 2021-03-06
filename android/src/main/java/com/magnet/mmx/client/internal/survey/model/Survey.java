/**
 * File generated by Magnet Magnet Lang Tool on Apr 11, 2016 1:09:57 PM
 * @see {@link http://developer.magnet.com}
 */

package com.magnet.mmx.client.internal.survey.model;

import com.magnet.max.android.User;

public class Survey {

  
  
  private String id;

  private String name;
  
  private SurveyDefinition surveyDefinition;

  
  private java.util.List<String> resultViewers;

  
  private java.util.List<String> participants;

  
  private java.util.List<String> owners;

  
  private java.util.Map<String, String> metaData;

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public SurveyDefinition getSurveyDefinition() {
    return surveyDefinition;
  }

  public java.util.List<String> getResultViewers() {
    return resultViewers;
  }

  public java.util.List<String> getParticipants() {
    return participants;
  }

  public java.util.List<String> getOwners() {
    return owners;
  }

  public java.util.Map<String, String> getMetaData() {
    return metaData;
  }

  public boolean hasResultsAccess() {
    return surveyDefinition.getResultAccessModel() == SurveyParticipantModel.PUBLIC ||
        (surveyDefinition.getResultAccessModel() == SurveyParticipantModel.PRIVATE &&
         owners.contains(User.getCurrentUserId()));
  }

  /**
  * Builder for Survey
  **/
  public static class SurveyBuilder {
    private Survey toBuild = new Survey();

    public SurveyBuilder() {
    }

    public Survey build() {
      return toBuild;
    }

    public SurveyBuilder id(String value) {
      toBuild.id = value;
      return this;
    }

    public SurveyBuilder name(String value) {
      toBuild.name = value;
      return this;
    }

    public SurveyBuilder surveyDefinition(SurveyDefinition value) {
      toBuild.surveyDefinition = value;
      return this;
    }

    public SurveyBuilder resultViewers(java.util.List<String> value) {
      toBuild.resultViewers = value;
      return this;
    }

    public SurveyBuilder participants(java.util.List<String> value) {
      toBuild.participants = value;
      return this;
    }

    public SurveyBuilder owners(java.util.List<String> value) {
      toBuild.owners = value;
      return this;
    }

    public SurveyBuilder metaData(java.util.Map<String, String> value) {
      toBuild.metaData = value;
      return this;
    }
  }
}
