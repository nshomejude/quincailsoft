ALTER TABLE sale_fulfillment
 ADD COLUMN method text CHECK (method IN ('PICKUP', 'DELIVERY')),
 ADD COLUMN scheduled_date date CHECK (scheduled_date >= business_date),
 ADD COLUMN delivery_address varchar(1000),
 ADD COLUMN recipient_name varchar(200),
 ADD COLUMN recipient_phone varchar(50),
 ADD COLUMN notes varchar(2000);
