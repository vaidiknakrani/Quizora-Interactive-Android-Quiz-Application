package com.example.adminquiz.models;

import com.example.adminquiz.entities.GhostEntity;
import com.example.adminquiz.entities.QuestionEntity;
import com.example.adminquiz.entities.QuizEntity;

import java.util.List;

/** Everything needed to replay a challenge on another phone (embedded in the share message). */
public class GhostPayload {
    public QuizEntity quiz;
    public List<QuestionEntity> questions;
    public GhostEntity ghost;
}
