package com.bluelinelabs.conductor;

import android.os.Bundle;
import android.view.ViewGroup;

import com.bluelinelabs.conductor.util.ActivityProxy;
import com.bluelinelabs.conductor.util.MockChangeHandler;
import com.bluelinelabs.conductor.util.TestController;
import com.bluelinelabs.conductor.util.TestMasterDetailController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class ReattachCaseTests {

    private ActivityProxy activityProxy;
    private Router router;

    public void createActivityController(Bundle savedInstanceState) {
        activityProxy = new ActivityProxy().create(savedInstanceState).start().resume();
        router = Conductor.attachRouter(activityProxy.getActivity(), activityProxy.getView(), savedInstanceState);
        if (!router.hasRootController()) {
            router.setRoot(RouterTransaction.with(new TestController()));
        }
    }

    @Before
    public void setup() {
        createActivityController(null);
    }

    @Test
    public void testNeedsAttachingOnPauseAndOrientation() {
        final TestController controllerA = new TestController();
        final TestController controllerB = new TestController();

        router.pushController(RouterTransaction.with(controllerA)
            .pushChangeHandler(MockChangeHandler.defaultHandler())
            .popChangeHandler(MockChangeHandler.defaultHandler()));

        assertTrue(controllerA.isAttached());
        assertFalse(controllerB.isAttached());

        sleepWakeDevice();

        assertTrue(controllerA.isAttached());
        assertFalse(controllerB.isAttached());

        router.pushController(RouterTransaction.with(controllerB)
                .pushChangeHandler(MockChangeHandler.defaultHandler())
                .popChangeHandler(MockChangeHandler.defaultHandler()));

        assertFalse(controllerA.isAttached());
        assertTrue(controllerB.isAttached());

        activityProxy.rotate();
        router.rebindIfNeeded();

        assertFalse(controllerA.isAttached());
        assertTrue(controllerB.isAttached());
    }

    @Test
    public void testChildNeedsAttachOnPauseAndOrientation() {
        final Controller controllerA = new TestController();
        final Controller childController = new TestController();
        final Controller controllerB = new TestController();

        router.pushController(RouterTransaction.with(controllerA)
                .pushChangeHandler(MockChangeHandler.defaultHandler())
                .popChangeHandler(MockChangeHandler.defaultHandler()));

        Router childRouter = controllerA.getChildRouter((ViewGroup)controllerA.getView().findViewById(TestController.VIEW_ID));
        childRouter.pushController(RouterTransaction.with(childController)
            .pushChangeHandler(MockChangeHandler.defaultHandler())
            .popChangeHandler(MockChangeHandler.defaultHandler()));

        assertTrue(controllerA.isAttached());
        assertTrue(childController.isAttached());
        assertFalse(controllerB.isAttached());

        sleepWakeDevice();

        assertTrue(controllerA.isAttached());
        assertTrue(childController.isAttached());
        assertFalse(controllerB.isAttached());

        router.pushController(RouterTransaction.with(controllerB)
                .pushChangeHandler(MockChangeHandler.defaultHandler())
                .popChangeHandler(MockChangeHandler.defaultHandler()));

        assertFalse(controllerA.isAttached());
        assertFalse(childController.isAttached());
        assertTrue(controllerB.isAttached());

        activityProxy.rotate();
        router.rebindIfNeeded();

        assertFalse(controllerA.isAttached());
        assertFalse(childController.isAttached());
        assertTrue(childController.getNeedsAttach());
        assertTrue(controllerB.isAttached());
    }

    @Test
    public void testMasterDetailNeedsAttachOnPauseAndOrientation() {
        final TestMasterDetailController controllerA = new TestMasterDetailController();
        final Controller childController1 = new TestController();
        final Controller childController2 = new TestController();
        final Controller controllerB = new TestController();

        router.pushController(RouterTransaction.with(controllerA)
                .pushChangeHandler(MockChangeHandler.defaultHandler())
                .popChangeHandler(MockChangeHandler.defaultHandler()));

        Router masterRouter = controllerA.getMasterRouter();
        masterRouter.pushController(RouterTransaction.with(childController1)
                .pushChangeHandler(MockChangeHandler.defaultHandler())
                .popChangeHandler(MockChangeHandler.defaultHandler()));

        Router detailRouter = controllerA.getDetailRouter();
        detailRouter.pushController(RouterTransaction.with(childController2)
                .pushChangeHandler(MockChangeHandler.defaultHandler())
                .popChangeHandler(MockChangeHandler.defaultHandler()));

        assertTrue(controllerA.isAttached());
        assertFalse(childController1.isAttached());
        assertTrue(childController2.isAttached());
        assertFalse(controllerB.isAttached());

        sleepWakeDevice();

        assertTrue(controllerA.isAttached());
        assertFalse(childController1.isAttached());
        assertTrue(childController2.isAttached());
        assertFalse(controllerB.isAttached());

        router.pushController(RouterTransaction.with(controllerB)
                .pushChangeHandler(MockChangeHandler.defaultHandler())
                .popChangeHandler(MockChangeHandler.defaultHandler()));

        assertFalse(controllerA.isAttached());
        assertFalse(childController1.isAttached());
        assertFalse(childController2.isAttached());
        assertTrue(controllerB.isAttached());

        activityProxy.rotate();
        router.rebindIfNeeded();

        assertFalse(controllerA.isAttached());
        assertFalse(childController1.isAttached());
        assertFalse(childController1.getNeedsAttach());
        assertFalse(childController2.isAttached());
        assertTrue(childController2.getNeedsAttach());
        assertTrue(controllerB.isAttached());
    }

    @Test
    public void testChildHandleBackOnOrientation() {
        final TestController controllerA = new TestController();
        final TestController controllerB = new TestController();
        final TestController childController = new TestController();

        router.pushController(RouterTransaction.with(controllerA)
                .pushChangeHandler(MockChangeHandler.defaultHandler())
                .popChangeHandler(MockChangeHandler.defaultHandler()));

        assertTrue(controllerA.isAttached());
        assertFalse(controllerB.isAttached());
        assertFalse(childController.isAttached());

        router.pushController(RouterTransaction.with(controllerB)
                .pushChangeHandler(MockChangeHandler.defaultHandler())
                .popChangeHandler(MockChangeHandler.defaultHandler()));

        Router childRouter = controllerB.getChildRouter((ViewGroup)controllerB.getView().findViewById(TestController.VIEW_ID));
        childRouter.setPopsLastView(true);
        childRouter.pushController(RouterTransaction.with(childController)
                .pushChangeHandler(MockChangeHandler.defaultHandler())
                .popChangeHandler(MockChangeHandler.defaultHandler()));

        assertFalse(controllerA.isAttached());
        assertTrue(controllerB.isAttached());
        assertTrue(childController.isAttached());

        activityProxy.rotate();
        router.rebindIfNeeded();

        assertFalse(controllerA.isAttached());
        assertTrue(controllerB.isAttached());
        assertTrue(childController.isAttached());

        router.handleBack();

        assertFalse(controllerA.isAttached());
        assertTrue(controllerB.isAttached());
        assertFalse(childController.isAttached());

        router.handleBack();

        assertTrue(controllerA.isAttached());
        assertFalse(controllerB.isAttached());
        assertFalse(childController.isAttached());
    }

    @Test
    public void testMasterDetailHandleBackOnOrientation() {
        TestController controllerA = new TestController();
        TestMasterDetailController controllerB = new TestMasterDetailController();
        TestController childController1 = new TestController();
        TestController childController2 = new TestController();

        router.pushController(RouterTransaction.with(controllerA)
                .tag("ControllerA")
                .pushChangeHandler(MockChangeHandler.defaultHandler())
                .popChangeHandler(MockChangeHandler.defaultHandler()));

        assertTrue(controllerA.isAttached());
        assertFalse(controllerB.isAttached());
        assertFalse(childController1.isAttached());
        assertFalse(childController2.isAttached());

        router.pushController(RouterTransaction.with(controllerB)
                .tag("ControllerB")
                .pushChangeHandler(MockChangeHandler.defaultHandler())
                .popChangeHandler(MockChangeHandler.defaultHandler()));
        assertFalse(controllerB.isTwoPanesMode());

        Router masterRouter = controllerB.getMasterRouter();
        masterRouter.setPopsLastView(true);
        masterRouter.pushController(RouterTransaction.with(childController1)
                .tag("Controller1")
                .pushChangeHandler(MockChangeHandler.defaultHandler())
                .popChangeHandler(MockChangeHandler.defaultHandler()));

        Router detailRouter = controllerB.getDetailRouter();
        detailRouter.setPopsLastView(true);
        detailRouter.pushController(RouterTransaction.with(childController2)
                .tag("Controller2")
                .pushChangeHandler(MockChangeHandler.defaultHandler())
                .popChangeHandler(MockChangeHandler.defaultHandler()));

        assertFalse(controllerA.isAttached());
        assertTrue(controllerB.isAttached());
        assertFalse(childController1.isAttached());
        assertTrue(childController2.isAttached());

        Bundle bundle = new Bundle();
        activityProxy.saveInstanceState(bundle);
        activityProxy.stop(true);
        activityProxy.rotate();

        router = Conductor.attachRouter(activityProxy.getActivity(), activityProxy.getView(), bundle);
        controllerA = (TestController) router.getControllerWithTag("ControllerA");
        controllerB = (TestMasterDetailController) router.getControllerWithTag("ControllerB");
        childController1 = (TestController) controllerB.getMasterRouter().getControllerWithTag("Controller1");
        childController2 = (TestController) controllerB.getDetailRouter().getControllerWithTag("Controller2");
        assertTrue(controllerB.isTwoPanesMode());

        activityProxy.start().resume();
        assertFalse(controllerA.isAttached());
        assertTrue(controllerB.isAttached());
        assertTrue(childController1.isAttached());
        assertTrue(childController2.isAttached());

        router.handleBack();

        assertFalse(controllerA.isAttached());
        assertTrue(controllerB.isAttached());
        assertTrue(childController1.isAttached());
        assertFalse(childController2.isAttached());

        router.handleBack();

        assertFalse(controllerA.isAttached());
        assertTrue(controllerB.isAttached());
        assertFalse(childController1.isAttached());
        assertFalse(childController2.isAttached());

        router.handleBack();
        assertTrue(controllerA.isAttached());
        assertFalse(controllerB.isAttached());
        assertFalse(childController1.isAttached());
        assertFalse(childController2.isAttached());
    }

    // Attempt to test https://github.com/bluelinelabs/Conductor/issues/86#issuecomment-231381271
    @Test
    public void testReusedChildRouterHandleBackOnOrientation() {
        TestController controllerA = new TestController();
        TestController controllerB = new TestController();
        TestController childController = new TestController();

        router.pushController(RouterTransaction.with(controllerA)
                .pushChangeHandler(MockChangeHandler.defaultHandler())
                .popChangeHandler(MockChangeHandler.defaultHandler()));

        assertTrue(controllerA.isAttached());
        assertFalse(controllerB.isAttached());
        assertFalse(childController.isAttached());

        router.pushController(RouterTransaction.with(controllerB)
                .pushChangeHandler(MockChangeHandler.defaultHandler())
                .popChangeHandler(MockChangeHandler.defaultHandler()));

        Router childRouter = controllerB.getChildRouter((ViewGroup)controllerB.getView().findViewById(TestController.VIEW_ID));
        childRouter.setPopsLastView(true);
        childRouter.pushController(RouterTransaction.with(childController)
                .pushChangeHandler(MockChangeHandler.defaultHandler())
                .popChangeHandler(MockChangeHandler.defaultHandler()));

        assertFalse(controllerA.isAttached());
        assertTrue(controllerB.isAttached());
        assertTrue(childController.isAttached());

        router.handleBack();

        assertFalse(controllerA.isAttached());
        assertTrue(controllerB.isAttached());
        assertFalse(childController.isAttached());

        childController = new TestController();
        childRouter.pushController(RouterTransaction.with(childController)
                .pushChangeHandler(MockChangeHandler.defaultHandler())
                .popChangeHandler(MockChangeHandler.defaultHandler()));

        assertFalse(controllerA.isAttached());
        assertTrue(controllerB.isAttached());
        assertTrue(childController.isAttached());

        activityProxy.rotate();
        router.rebindIfNeeded();

        assertFalse(controllerA.isAttached());
        assertTrue(controllerB.isAttached());
        assertTrue(childController.isAttached());

        router.handleBack();

        childController = new TestController();
        childRouter.pushController(RouterTransaction.with(childController)
                .pushChangeHandler(MockChangeHandler.defaultHandler())
                .popChangeHandler(MockChangeHandler.defaultHandler()));

        assertFalse(controllerA.isAttached());
        assertTrue(controllerB.isAttached());
        assertTrue(childController.isAttached());

        router.handleBack();

        assertFalse(controllerA.isAttached());
        assertTrue(controllerB.isAttached());
        assertFalse(childController.isAttached());

        router.handleBack();

        assertTrue(controllerA.isAttached());
        assertFalse(controllerB.isAttached());
        assertFalse(childController.isAttached());
    }

    @Test
    public void testReusedDetailRouterHandleBackOnOrientation() {
        TestController controllerA = new TestController();
        TestMasterDetailController controllerB = new TestMasterDetailController();
        TestController childController = new TestController();

        router.pushController(RouterTransaction.with(controllerA)
                .pushChangeHandler(MockChangeHandler.defaultHandler())
                .popChangeHandler(MockChangeHandler.defaultHandler()));

        assertTrue(controllerA.isAttached());
        assertFalse(controllerB.isAttached());
        assertFalse(childController.isAttached());

        router.pushController(RouterTransaction.with(controllerB)
                .pushChangeHandler(MockChangeHandler.defaultHandler())
                .popChangeHandler(MockChangeHandler.defaultHandler()));

        Router masterRouter = controllerB.getMasterRouter();
        masterRouter.setPopsLastView(true);

        Router childRouter = controllerB.getDetailRouter();
        childRouter.setPopsLastView(true);
        childRouter.pushController(RouterTransaction.with(childController)
                .pushChangeHandler(MockChangeHandler.defaultHandler())
                .popChangeHandler(MockChangeHandler.defaultHandler()));

        assertFalse(controllerA.isAttached());
        assertTrue(controllerB.isAttached());
        assertTrue(childController.isAttached());

        router.handleBack();

        assertFalse(controllerA.isAttached());
        assertTrue(controllerB.isAttached());
        assertFalse(childController.isAttached());

        childController = new TestController();
        childRouter.pushController(RouterTransaction.with(childController)
                .pushChangeHandler(MockChangeHandler.defaultHandler())
                .popChangeHandler(MockChangeHandler.defaultHandler()));

        assertFalse(controllerA.isAttached());
        assertTrue(controllerB.isAttached());
        assertTrue(childController.isAttached());

        activityProxy.rotate();
        router.rebindIfNeeded();

        assertFalse(controllerA.isAttached());
        assertTrue(controllerB.isAttached());
        assertTrue(childController.isAttached());

        router.handleBack();

        childController = new TestController();
        childRouter.pushController(RouterTransaction.with(childController)
                .pushChangeHandler(MockChangeHandler.defaultHandler())
                .popChangeHandler(MockChangeHandler.defaultHandler()));

        assertFalse(controllerA.isAttached());
        assertTrue(controllerB.isAttached());
        assertTrue(childController.isAttached());

        router.handleBack();

        assertFalse(controllerA.isAttached());
        assertTrue(controllerB.isAttached());
        assertFalse(childController.isAttached());

        router.handleBack();

        assertTrue(controllerA.isAttached());
        assertFalse(controllerB.isAttached());
        assertFalse(childController.isAttached());
    }


    // Attempt to test https://github.com/bluelinelabs/Conductor/issues/367
    @Test
    public void testViewIsAttachedAfterStartedActivityIsRecreated() {
        Controller controller1 = new TestController();
        Controller controller2 = new TestController();

        router.setRoot(RouterTransaction.with(controller1));
        assertTrue(controller1.isAttached());

        // Lock screen
        Bundle bundle = new Bundle();
        activityProxy.pause().saveInstanceState(bundle).stop(false);

        // Push a 2nd controller, which will rotate the screen once it unlocked
        router.pushController(RouterTransaction.with(controller2));
        assertTrue(controller2.isAttached());
        assertTrue(controller2.getNeedsAttach());

        // Unlock screen and rotate
        activityProxy.start();
        activityProxy.rotate();

        assertTrue(controller2.isAttached());
    }

    @Test
    public void testPopMiddleControllerAttaches() {
        Controller controller1 = new TestController();
        Controller controller2 = new TestController();
        Controller controller3 = new TestController();

        router.setRoot(RouterTransaction.with(controller1));
        router.pushController(RouterTransaction.with(controller2));
        router.pushController(RouterTransaction.with(controller3));
        router.popController(controller2);

        assertFalse(controller1.isAttached());
        assertFalse(controller2.isAttached());
        assertTrue(controller3.isAttached());

        controller1 = new TestController();
        controller2 = new TestController();
        controller3 = new TestController();

        router.setRoot(RouterTransaction.with(controller1));
        router.pushController(RouterTransaction.with(controller2));
        router.pushController(RouterTransaction.with(controller3).pushChangeHandler(MockChangeHandler.noRemoveViewOnPushHandler()));
        router.popController(controller2);

        assertTrue(controller1.isAttached());
        assertFalse(controller2.isAttached());
        assertTrue(controller3.isAttached());
    }

    private void sleepWakeDevice() {
        activityProxy.saveInstanceState(new Bundle()).pause();
        activityProxy.resume();
    }

}
