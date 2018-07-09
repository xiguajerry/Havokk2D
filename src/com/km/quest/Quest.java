package com.km.quest;

public interface Quest {

    public boolean isActive();

    public boolean checkRequirements();

    public int getCurrentStep();

    public void start();

    public void next();

    public boolean isComplete();

    public void finish();

}
