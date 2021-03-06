package MobileSensors.Events.Trainers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.core.Instances;
import weka.core.SerializationHelper;

import MobileSensors.Events.ARFactories.ARFactory;
import MobileSensors.Events.Labels.EventLabel;
import MobileSensors.Sensors.Accelerometer;
import MobileSensors.Sensors.Location;
import MobileSensors.Sensors.SensorRecord;
import MobileSensors.Helpers.DataWindowBuilder;

public abstract class EventTrainer<L extends EventLabel> {
	
	/*
	private class WindowBuilder<S extends Sensor> {
		
		public ArrayList<ArrayList<S>> buildWindows (
				ArrayList<S> values,
				int windowWidth,
				int windowDelta) {
			
			ArrayList<ArrayList<S>> result = new ArrayList<ArrayList<S>>();
			
			windowWidth = windowWidth > 1 ? windowWidth : 1;
			windowDelta = windowDelta > 1 ? windowDelta : 1;
			
			int i = 0;
			
			while (i <= values.size() - windowWidth) {
				
				ArrayList<S> window = new ArrayList<S>();
				
				int j = i;
				
				while (j - i < windowWidth) {
					
					window.add(values.get(j));
					
					j++;
					
				}
				
				result.add(window);
				
				i += windowDelta;
				
			}

			return result;
			
		}
		
	}
	*/

	private ARFactory<L> arFactory;
	private Classifier eventClassifier;
	private Evaluation eventClassifierEvaluation;
	
	private int windowWidth;
	private int windowDelta;
	private int validationFolds;
	
	private File modelFile;
	private File arffFile;
	private File evalFile;
	private File clsfrFile;
	
	protected Map<SensorRecord, L> sensorCollections;
	
	public EventTrainer (ARFactory<L> arFactory, Classifier eventClassifier, File modelFile,
			int windowWidth, int windowDelta, int validationFolds) {
		
		this.arFactory = arFactory;
		this.eventClassifier = eventClassifier;
		
		this.windowWidth = windowWidth;
		this.windowDelta = windowDelta;
		this.validationFolds = validationFolds;
		
		Path modelDir = Paths.get(modelFile.getParent());
		
		this.modelFile = modelFile;
		this.arffFile = modelDir.resolve(FilenameUtils.getBaseName(modelFile.getName()) + ".arff").toFile();
		this.evalFile = modelDir.resolve(FilenameUtils.getBaseName(modelFile.getName()) + ".eval").toFile();
		this.clsfrFile = modelDir.resolve(FilenameUtils.getBaseName(modelFile.getName()) + ".txt").toFile();
		
		
	}
	
	private void log (String msg) {
		
		System.out.println(this.getClass().getSimpleName() + " :: " + msg);
		
	}
	
	private void logDuration (Date start, Date stop) {
		
		this.log("Done after " + DurationFormatUtils.formatDuration(stop.getTime() - start.getTime(), "HH:mm:ss"));
		
	}
	
	protected Instances buildTrainingSet (Map<SensorRecord, L> sensorCollections, int windowWidth, int windowDelta) throws IOException {
		
		Date start;
		Date stop;
		
		start = new Date();
		
		//============================================================================================================
		// Variable Declarations:
		
		DataWindowBuilder<Accelerometer> accWindowBuilder;		// WindowBuilder for accelerometer sensor data
		DataWindowBuilder<Location> locWindowBuilder;			// WindowBuilder for location sensor data
		
		ArrayList<ArrayList<Accelerometer>> accWindows;		// List of accelerometer windows
		ArrayList<ArrayList<Location>> locWindows;			// List of location windows
		
		ArrayList<Accelerometer> accWindow;					// Accelerometer window
		ArrayList<Location> locWindow;						// Location window
		
		L label;											
		
		Map<SensorRecord, L> newSensorCollections;

		//============================================================================================================
		// Algorithm:
		
		accWindowBuilder = new DataWindowBuilder<Accelerometer>();
		locWindowBuilder = new DataWindowBuilder<Location>();
		
		newSensorCollections = new HashMap<SensorRecord, L>();
		
		this.log("Building windows");
		
		for (SensorRecord oldSc : sensorCollections.keySet()) {
			
			label = sensorCollections.get(oldSc);
			
			accWindows = accWindowBuilder.buildWindows(oldSc.getAcceleration(), windowWidth, windowDelta);
//			locWindows = locWindowBuilder.buildWindows(oldSc.getLocation(), windowWidth, windowDelta);
			
			for (int i=0; i < accWindows.size(); i++) {
				
				accWindow = i < accWindows.size() ? accWindows.get(i) : new ArrayList<Accelerometer>();
//				locWindow = i < locWindows.size() ? locWindows.get(i) : new ArrayList<Location>();
				
				SensorRecord newSc = new SensorRecord();
				
				newSc.setAcceleration(accWindow);
//				newSc.setLocation(locWindow);
				
				newSensorCollections.put(newSc, label);
				
			}
			
			
			
		}
		
		//============================================================================================================
		
		Instances trainingSet = this.arFactory.createTrainingSet(newSensorCollections);
		
		this.log("Writing training set to " + this.arffFile.getCanonicalPath());
		
		BufferedWriter arffWriter = new BufferedWriter(new FileWriter(this.arffFile));
		arffWriter.write(trainingSet.toString());
		arffWriter.flush();
		arffWriter.close();
		
		stop = new Date();
		
		this.logDuration(start, stop);
		
		return trainingSet;
		
	}
	
	protected void buildClassifier (Instances trainingSet) throws Exception {
		
		Date start;
		Date stop;
		
		start = new Date();
		
		this.log("Building new " + this.eventClassifier.getClass().getName() + " classifier.");
		
		this.eventClassifier.buildClassifier(trainingSet);
		
		this.log("Writing classifier to " + this.modelFile.getCanonicalPath());
		
		SerializationHelper.write(this.modelFile.getCanonicalPath(), this.eventClassifier);
		
		BufferedWriter evalWriter = new BufferedWriter(new FileWriter(this.clsfrFile));
		evalWriter.write(this.eventClassifier.toString());
		evalWriter.flush();
		evalWriter.close();
		
		stop = new Date();


		this.logDuration(start, stop);
		
	}
	
	protected void crossValidateModel (Instances trainingSet, int folds) throws Exception {
		
		Date start;
		Date stop;
		
		start = new Date();
		
		this.log("Starting cross validation with " + folds + " folds.");
		
		Evaluation eval = new Evaluation(trainingSet);
		
		eval.crossValidateModel(this.eventClassifier, trainingSet, folds, new Random(1));
		
		this.log("Finished cross validation.");
		
		this.log("Writing evaluation to " + this.evalFile.getCanonicalPath());
		
		BufferedWriter evalWriter = new BufferedWriter(new FileWriter(this.evalFile));
		evalWriter.write("Summary:");
		evalWriter.newLine();
		evalWriter.write("========");
		evalWriter.newLine();
		evalWriter.write(eval.toSummaryString());
		evalWriter.newLine();
		evalWriter.write("Detailed Statistics:");
		evalWriter.newLine();
		evalWriter.write("====================");
		evalWriter.newLine();
		evalWriter.write(eval.toClassDetailsString());
		evalWriter.newLine();
		evalWriter.write("Confusion matrix :");
		evalWriter.newLine();
		evalWriter.write("==================");
		evalWriter.newLine();
		evalWriter.write(eval.toMatrixString());
		evalWriter.newLine();
		evalWriter.newLine();
		evalWriter.write(this.eventClassifier.toString());
		evalWriter.flush();
		evalWriter.close();
		
		stop = new Date();


		this.logDuration(start, stop);
		
	}
	
	public void train (Map<SensorRecord, L> sensorCollections) throws Exception {
		
		this.train(sensorCollections, false);
		
	}
	
	public void train (Map<SensorRecord, L> sensorCollections, boolean validate) throws Exception {
		
		Date start;
		Date stop;
		
		start = new Date();
		
		System.out.println();
		
		this.log("Started training!");
		
		Instances trainingSet = this.buildTrainingSet(sensorCollections, this.windowWidth, this.windowDelta);
		
		this.buildClassifier(trainingSet);
		
		if (validate) {
			
			this.crossValidateModel(trainingSet, this.validationFolds);
			
		}
		
		this.log("Finished training!");
		
		System.out.println();
		
		stop = new Date();
		
		this.logDuration(start, stop);
	}
	
}
