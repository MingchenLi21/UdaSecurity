package Service;

import Application.StatusListener;
import Data.AlarmStatus;
import Data.ArmingStatus;
import Data.SecurityRepository;
import Data.Sensor;
import Image_service.ImageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SecurityServiceTest {

    @Mock
    private SecurityRepository mockedSecurityRepository;

    @Mock
    private Sensor mockedSensor;

    @Mock
    private ImageService imageService;

    @Mock
    private BufferedImage image;

    @Mock
    private StatusListener listener;

    private Set<Sensor> getMockedSensors() {
        Set<Sensor> sensors = new HashSet<>();
        sensors.add(mockedSensor);
        return sensors;
    }

    SecurityService service;

    @BeforeEach
    void init(){
        service = new SecurityService(mockedSecurityRepository, imageService);
    }


    @Test //1
    void handleSensorActivated_armed_sensorActivated_toPending(){

        when(mockedSecurityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(mockedSecurityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);

        service.handleSensorActivated();
        verify(mockedSecurityRepository).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    @Test //2
    void handleSensorActivated_armed_sensorActivated_alarmPending_toAlarm(){
        when(mockedSecurityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_AWAY);
        when(mockedSecurityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        service.handleSensorActivated();
        verify(mockedSecurityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test //3 ?
    void handleSensorDeactivated_pending_deactivated_toNoAlarm(){
        when(mockedSecurityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        service.handleSensorDeactivated();
        verify(mockedSecurityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @ParameterizedTest //4 ?
    @ValueSource(booleans = {true, false})
    void changeSensorActivationStatus_actived_notChangealarm(Boolean active){

        when(mockedSensor.getActive()).thenReturn(active);

        service.changeSensorActivationStatus(mockedSensor, active);
        verify(mockedSecurityRepository,never()).setAlarmStatus(any());
    }


    @Test //5 ?
    void changeSensorActivationStatus_activated_pending_toAlarm(){
        when(mockedSensor.getActive()).thenReturn(true);
        when(mockedSecurityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        service.changeSensorActivationStatus(mockedSensor, true);
        verify(mockedSecurityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test // 6 If a sensor is deactivated while already inactive, make no changes to the alarm state.
    void changeSensorActivationStatus_inactive_NotChangeAlarm() {
        when(mockedSensor.getActive()).thenReturn(false);
        service.changeSensorActivationStatus(mockedSensor, false);
        verify(mockedSecurityRepository, never()).setAlarmStatus(any());
    }

    @Test //7 If the image service identifies an image containing a cat while the system is armed-home, put the system into alarm status.

    void processImage_cat_armedHome_toAlarm(){
        when(mockedSecurityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(imageService.imageContainsCat(image, 50.0f)).thenReturn(true);

        service.processImage(image);
        verify(mockedSecurityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test //8 If the image service identifies an image that does not contain a cat, change the status to no alarm as long as the sensors are not active.
    void processImage_noCat_inAcitive_toNoAlarm(){
        when(imageService.imageContainsCat(image, 50.0f)).thenReturn(false);
        when(mockedSecurityRepository.anyActive()).thenReturn(false);

        service.processImage(image);
        verify(mockedSecurityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @Test //9 If the system is disarmed, set the status to no alarm.
    void setArmingStatus_setDisarmed_toNoAlarm(){

        service.setArmingStatus(ArmingStatus.DISARMED);
        verify(mockedSecurityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @Test //10 If the system is armed, reset all sensors to inactive.
    void setArmingStatus_setArmed_toSensorsInactive(){

        when(mockedSecurityRepository.getSensors()).thenReturn(getMockedSensors());

        service.setArmingStatus(ArmingStatus.ARMED_HOME);

        verify(mockedSensor).setActive(false);
    }

    @Test //11 If the system is armed-home while the camera shows a cat, set the alarm status to alarm.
    void setArmingStatus_cat_ArmedHome_toAlarm(){
        when(mockedSecurityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(imageService.imageContainsCat(image, 50.0f)).thenReturn(true);

        service.processImage(image);
        service.setArmingStatus(ArmingStatus.ARMED_HOME);
        verify(mockedSecurityRepository, atLeast(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    void aboutRest(){
        service.addSensor(mockedSensor);
        service.getSensors();
        service.removeSensor(mockedSensor);
        service.addStatusListener(listener);
        service.removeStatusListener(listener);

        verify(mockedSecurityRepository).addSensor(mockedSensor);
        verify(mockedSecurityRepository).getSensors();
        verify(mockedSecurityRepository).removeSensor(mockedSensor);
    }

    @Test
    void aboutRest2(){
        when(mockedSecurityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        service.handleSensorDeactivated();
        //verify(mockedSecurityRepository).setAlarmStatus(AlarmStatus.PENDING_ALARM);

    }

    @Test
    void aboutRest3(){
        when(service.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);

        when(mockedSensor.getActive()).thenReturn(false);
        service.changeSensorActivationStatus(mockedSensor, true);
        verify(mockedSecurityRepository).getAlarmStatus();

        when(mockedSensor.getActive()).thenReturn(true);
        service.changeSensorActivationStatus(mockedSensor, false);
        verify(mockedSecurityRepository).getArmingStatus();
    }
}
