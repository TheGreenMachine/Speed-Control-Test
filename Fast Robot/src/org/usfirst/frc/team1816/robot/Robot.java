
package org.usfirst.frc.team1816.robot;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import org.usfirst.frc.team1816.robot.commands.ExampleCommand;
import org.usfirst.frc.team1816.robot.subsystems.ExampleSubsystem;

import com.ctre.CANTalon;
import com.ctre.CANTalon.TalonControlMode;
import com.ctre.CANTalon.FeedbackDevice;
import com.kauailabs.navx.frc.AHRS;

import edu.wpi.first.wpilibj.AnalogInput;
import edu.wpi.first.wpilibj.Encoder;
import edu.wpi.first.wpilibj.IterativeRobot;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.Joystick.AxisType;
import edu.wpi.first.wpilibj.RobotDrive;
import edu.wpi.first.wpilibj.SPI;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.Ultrasonic;
import edu.wpi.first.wpilibj.command.Command;
import edu.wpi.first.wpilibj.command.Scheduler;
import edu.wpi.first.wpilibj.livewindow.LiveWindow;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

/**
 * The VM is configured to automatically run this class, and to call the
 * functions corresponding to each mode, as described in the IterativeRobot
 * documentation. If you change the name of this class or the package after
 * creating this project, you must also update the manifest file in the resource
 * directory.
 */
public class Robot extends IterativeRobot {
	public static final ExampleSubsystem exampleSubsystem = new ExampleSubsystem();
	public static OI oi;
	Command autonomousCommand;
	SendableChooser<Command> chooser = new SendableChooser<>();
	File f;
	BufferedWriter bw;
	FileWriter fw;
	Ultrasonic ultra;
	private AHRS gyro = new AHRS(SPI.Port.kMXP);
	double speed = -.5;

	CANTalon frontLeftMotor = new CANTalon(1); /* device IDs here (1 of 2) */
	CANTalon frontRightMotor = new CANTalon(4);

	CANTalon leftSlave = new CANTalon(2);
	CANTalon rightSlave = new CANTalon(3);

	RobotDrive _drive = new RobotDrive(frontLeftMotor, frontRightMotor);

	Joystick _joy = new Joystick(0);
	AnalogInput ai;
	double bits;

	CANTalon enc1 = new CANTalon(1);
	CANTalon enc2 = new CANTalon(4);

	double autoNum = 0;
	double target = 25000;

	public Logging logging;
	String a;
	long t0;
	long tn;
	StringBuilder sb = new StringBuilder();

	/**
	 * This function is run when the robot is first started up and should be
	 * used for any initialization code.
	 */
	@Override
	public void robotInit() {
		oi = new OI();
		chooser.addDefault("Default Auto", new ExampleCommand());
		// chooser.addObject("My Auto", new MyAutoCommand());
		SmartDashboard.putData("Auto mode", chooser);
		ultra = new Ultrasonic(0, 1);
		ultra.setAutomaticMode(true);
		// frontLeftMotor.setFeedbackDevice(FeedbackDevice.QuadEncoder);
		// frontLeftMotor.reverseSensor(false);
		// frontLeftMotor.configEncoderCodesPerRev(42574);
		// frontLeftMotor.configNominalOutputVoltage(+0.0f, -0.0f);
		// frontLeftMotor.configPeakOutputVoltage(+12.0f, -12.0f);

		// frontLeftMotor.setProfile(0);
		// frontLeftMotor.setF(0);
		// frontLeftMotor.setP(.1);
		// frontLeftMotor.setI(0);
		// frontLeftMotor.setD(0);

		ai = new AnalogInput(3);

		ai.setOversampleBits(4);
		ai.setAverageBits(2);
		bits = ai.getAverageVoltage();
		AnalogInput.setGlobalSampleRate(62500);
		frontLeftMotor.changeControlMode(TalonControlMode.PercentVbus);
		frontRightMotor.changeControlMode(TalonControlMode.PercentVbus);
		leftSlave.changeControlMode(TalonControlMode.Follower);
		rightSlave.changeControlMode(TalonControlMode.Follower);

		leftSlave.set(1);
		rightSlave.set(4);

		t0 = System.currentTimeMillis();
		logging = new Logging("ticks");

		// int ticks = _drive.getEncPosition();

	}

	public void ultrasonicSample() {
		double range = ultra.getRangeInches(); // reads the range on the
												// ultrasonic sensor
		System.out.println(range);
	}

	/**
	 * This function is called once each time the robot enters Disabled mode.
	 * You can use it to reset any subsystem information you want to clear when
	 * the robot is disabled.
	 */
	@Override
	public void disabledInit() {

	}

	@Override
	public void disabledPeriodic() {
		Scheduler.getInstance().run();
		logging.close();
	}

	/**
	 * This autonomous (along with the chooser code above) shows how to select
	 * between different autonomous modes using the dashboard. The sendable
	 * chooser code works with the Java SmartDashboard. If you prefer the
	 * LabVIEW Dashboard, remove all of the chooser code and uncomment the
	 * getString code to get the auto name from the text box below the Gyro
	 *
	 * You can add additional auto modes by adding additional commands to the
	 * chooser code above (like the commented example) or additional comparisons
	 * to the switch structure below with additional strings & commands.
	 */
	@Override
	public void autonomousInit() {
		autonomousCommand = chooser.getSelected();
		speed = -.5;
		/*
		 * String autoSelected = SmartDashboard.getString("Auto Selector",
		 * "Default"); switch(autoSelected) { case "My Auto": autonomousCommand
		 * = new MyAutoCommand(); break; case "Default Auto": default:
		 * autonomousCommand = new ExampleCommand(); break; }
		 */

		// schedule the autonomous command (example)
		if (autonomousCommand != null)
			autonomousCommand.start();
		enc1.setEncPosition(0);
		enc2.setEncPosition(0);

		autoNum = -.5;
		a = Double.toString(enc2.getEncPosition()) + "," + Double.toString(-enc1.getEncPosition());
		// logging.log(a+","+(t0-tn));
	}

	/**
	 * This function is called periodically during autonomous
	 */
	@Override
	public void autonomousPeriodic() {
		Scheduler.getInstance().run();

		double currentLeftValue = frontLeftMotor.getEncPosition();
		double currentHeading = gyro.getAngle();

		while (isAutonomous() && isEnabled()) {
			_drive.arcadeDrive(autoNum, 0);
			Timer.delay(.01);
			System.out.println("enc1: " + enc1.getEncPosition());
			System.out.println(a);
			System.out.println("ai: " + ai.getAverageVoltage());
			System.out.println("AutoNum: " + autoNum);
			// if(ai.getAverageVoltage()>.6){
			// autoNum = 0;
			// tn = System.currentTimeMillis();
			// logging.close();
			// }
		}

	}

	@Override
	public void teleopInit() {
		// This makes sure that the autonomous stops running when
		// teleop starts running. If you want the autonomous to
		speed = -.5;
		// autonomousCommand.cancel();

	}

	/**
	 * This function is called periodically during operator control
	 */
	@Override
	public void teleopPeriodic() {
		bits = ai.getAverageVoltage();
		Scheduler.getInstance().run();
		ultrasonicSample();
		System.out.println(ai.getAverageVoltage());
		logging.log(_joy.getRawButton(1) + "," + frontLeftMotor.getSpeed());
		_drive.arcadeDrive(speed,0);
		System.out.println("speed: "+speed);
		//_drive.arcadeDrive(_joy);
		
		// double leftYstick = _joy.getAxis(AxisType.kY);
		// double motorOutput = frontLeftMotor.getOutputVoltage() /
		// frontLeftMotor.getBusVoltage();

		// if(_joy.getRawButton(1)){
		// /* Speed mode */
		// double targetSpeed = leftYstick * 1500.0; /* 1500 RPM in either
		// direction */
		// frontLeftMotor.changeControlMode(TalonControlMode.Speed);
		// frontLeftMotor.set(targetSpeed); /* 1500 RPM in either direction */
		// } else {
		// /* Percent voltage mode */
		// frontLeftMotor.changeControlMode(TalonControlMode.PercentVbus);
		// frontLeftMotor.set(leftYstick);
		// }

		if (bits > .6) {
			if (bits > 1.0) {
				speed = 0;
			} else {
				speed = -(1 - ai.getAverageVoltage() * 1.5);
			}
		} else {
			speed = -.5;
		}
	}

	/**
	 * This function is called periodically during test mode
	 */
	@Override
	public void testPeriodic() {
		LiveWindow.run();
	}
}
