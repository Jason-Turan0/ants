###### 9-8-2020

Ran the following models. Dense was clearly inferior. 10% below all the conv2d for categorical accuracy.
best performing was 
   models = [
            mf.create_dense_2d_model(0.0005, bot_to_emulate),
            mf.create_dense_2d_model(0.001, bot_to_emulate),
            mf.create_dense_2d_model(0.002, bot_to_emulate),
            mf.create_dense_2d_model(0.01, bot_to_emulate),

            mf.create_conv_2d_model(0.0005, 1, bot_to_emulate),
            mf.create_conv_2d_model(0.001, 1, bot_to_emulate),
            mf.create_conv_2d_model(0.002, 1, bot_to_emulate),
            mf.create_conv_2d_model(0.01, 1, bot_to_emulate),

            mf.create_conv_2d_model(0.0005, 2, bot_to_emulate),
            mf.create_conv_2d_model(0.001, 2, bot_to_emulate),
            mf.create_conv_2d_model(0.002, 2, bot_to_emulate),
            mf.create_conv_2d_model(0.01, 2, bot_to_emulate),
        ]
        
   ![](TestRun%209-9-2020.png)