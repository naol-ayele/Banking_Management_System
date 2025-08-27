from backend.services.analytics_service import demo_default_risk_training

def test_demo_default_risk_training():
    coefs, intercept = demo_default_risk_training()
    assert len(coefs) == 3
