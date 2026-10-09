package com.example.adminquiz.models;

import com.example.adminquiz.entities.AnswerEntity;
import com.example.adminquiz.entities.AttemptEntity;

import java.util.List;

public class ResultData {
    public AttemptEntity attempt;
    public List<AnswerEntity> answers;
    public GhostItem ghost;
}
