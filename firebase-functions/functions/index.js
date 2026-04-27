const functions = require('firebase-functions');
const admin = require('firebase-admin');
const axios = require('axios');

admin.initializeApp();

exports.analyzeParking = functions.storage.object().onFinalize(async (object) => {
    if (!object.contentType.startsWith('image/')) {
        console.log('not an image, skipping analysis');
        return null;
    }

    const bucket = admin.storage().bucket(object.bucket);
    const file = bucket.file(object.name);

    const [url] = await file.getSignedUrl({
        action: 'read',
        expires: '03-09-2491'
    });

    console.log('Analyzing image:', url);

    try {
        const response = await axios.post(
            'https://detect.roboflow.com/parking-zu3pa/2', 
            null, 
            {
                params: {
                    api_key: 'puSnkxAqJhy0AO9uZHGF',
                    image: url
                }
            }
        );

        const predictions = response.data.predictions;
        const totalSpots = 20; 
        const occupiedSpots = predictions.length;
        const freeSpots = totalSpots - occupiedSpots;

        await admin.database().ref('parking').set({
            free: freeSpots,
            occupied: occupiedSpots,
            total: totalSpots,
            lastUpdate: admin.database.ServerValue.TIMESTAMP,
            confidence: predictions.length > 0 ? Math.round(predictions[0].confidence * 100) : 0
        });

        console.log(`Successful analysis: Free: ${freeSpots}, Occupied: ${occupiedSpots}`);

    } catch (error) {
        console.error('Error analyzing with Roboflow:', error.message);
    }

    return null;
});
