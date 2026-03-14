const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp();

exports.notifyDonorsOnNewRequest = functions.firestore
    .document('blood_requests/{requestId}')
    .onCreate(async (snapshot, context) => {
        const requestData = snapshot.data();
        const isEmergency = requestData.isEmergency;
        const bloodType = requestData.bloodType;
        const reqLat = requestData.latitude;
        const reqLon = requestData.longitude;

        // Find all donors
        const donorsSnapshot = await admin.firestore().collection('users')
            .where('role', '==', 'donor').get();

        const notifications = [];

        donorsSnapshot.forEach(doc => {
            const donor = doc.data();
            const token = donor.fcmToken;
            if (!token) return;

            let shouldNotify = false;
            let title = "Blood Needed";
            let body = `${bloodType} blood requested near your area.`;

            if (isEmergency) {
                // Algorithm: Within 10km for Emergency
                const dist = calculateDistance(reqLat, reqLon, donor.latitude, donor.longitude);
                if (dist <= 10) {
                    shouldNotify = true;
                    title = "EMERGENCY";
                    body = "Urgent blood request near you!";
                }
            } else if (donor.bloodType === bloodType) {
                // Algorithm: Matching Blood Group for Normal
                shouldNotify = true;
            }

            if (shouldNotify) {
                notifications.push(admin.messaging().send({
                    token: token,
                    notification: { title, body },
                    data: { requestId: snapshot.id }
                }));
            }
        });

        return Promise.all(notifications);
    });

// Helper for Distance algorithm
function calculateDistance(lat1, lon1, lat2, lon2) {
    const p = 0.017453292519943295; 
    const c = Math.cos;
    const a = 0.5 - c((lat2 - lat1) * p)/2 + 
            c(lat1 * p) * c(lat2 * p) * 
            (1 - c((lon2 - lon1) * p))/2;
    return 12742 * Math.asin(Math.sqrt(a)); 
}