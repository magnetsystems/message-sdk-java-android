/**
 * Copyright (c) 2012-2016 Magnet Systems. All rights reserved.
 */
package com.magnet.mmx.client.ext.survey;

public class SurveyAggregatableQuestionOption<T, V> extends SurveyQuestionOption<T>  {
  private V result;

  public SurveyAggregatableQuestionOption(String text) {
    super(text);
  }

}
