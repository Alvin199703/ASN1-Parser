/*
 * Generated by ASN.1 resolver
 * From ASN.1 module "MyMoudle"
 * 	found in "MyMoudle.txt"
 */
#inlcude"asn.h"
typedef INTEGER_t Height_t;
typedef struct Area{
	INTEGER_t length;
	INTEGER_t width;
	Height_t hhh;
}Area_t;
typedef enum Temp2{
	Temp2_sb = 1;
	Temp2_xswl = 2;
} e_Temp2;
typedef struct Temp1{
	Date_t saturday;
	Date_t sunday;
}Temp1_t;
typedef ErrorCode_t DiskFull_t
typedef BIT_STRING_t Job_t
typedef Job_t Temp0_t;
typedef long Age_t;
typedef long Year_t;
typedef long Weekday_t;
INTEGER_t one = 1;
OCTET_STRING_t two = "2";
INTEGER_t fuyi = -1;

typedef IA5String_t IString_t
typedef OCTET_STRING_t TString_t
typedef struct Month {
	A_SEQUENCE_OF(IA5String_t) list;
}Month_t
typedef long Weekend_t;
typedef OCTET_STRING_t Name_t;
typedef struct Names {
	A_SET_OF(Name_t) list;
}Names_t
Tired_t ::= Temp0_t;
typedef Job_t Enterprise_t;
typedef REAL_t Salary_t;
typedef NULL_t Holiday_t;
typedef BOOLEAN_t Sex_t;
typedef utcTime_t Date_t;
typedef printableString_t Order_t;
typedef enum ErrorCode {
	ErrorCode_disk_full = -1;
	ErrorCode_no_dist = 1;
}e_ErrorCode;
typedef ErrorCode_t DiskError_t
typedef struct More{
	Area_t aaa;
}More_t;
typedef struct Rectangle{
	Height_t *height /* OPTIONAL */;
	Area_t area;
	INTEGER_t length;
	INTEGER_t width;
	Height_t hhh;
	Temp1_t weekend;
	e_Temp2 test;
	INTEGER_t *author /* DEFAULT 0 */;
	OCTET_STRING_t title;
	/*
	 * This type is extensible,
	 * possible extensions are below.
	 */
}Rectangle_t;

typedef union Test{
	INTEGER_t year;
	BIT_STRING_t month;
}Test_t;
int Year_constraint(const void* sptr){
	long value;
	if(!sptr){
		printf("value not given");
		return -1;
	}
	value = *(const long *)sptr;
	if((value == 2019)) {
		return 0;  //constraint success
	}
	else {
		printf("constraint failed");
		return -1;
	}
}
int Weekday_constraint(const void* sptr){
	long value;
	if(!sptr){
		printf("value not given");
		return -1;
	}
	value = *(const long *)sptr;
	if((value > 1 && value <= 5)) {
		return 0;  //constraint success
	}
	else {
		printf("constraint failed");
		return -1;
	}
}
int IString_constraint(const void* sptr){
	long value;
	if(!sptr){
		printf("value not given");
		return -1;
	}
	const IA5String_t *st = (const IA5String_t *)sptr;
	const uint8_t *ch = st->buf;
	const uint8_t *end = ch + st->size;
	
	for(; ch < end; ch++) {
		uint8_t cv = *ch;
		if( !( cv >= 48 && cv <= 57) || cv != 42 || cv != 35) 
			return -1;
	}
	return 0;
}
int TString_constraint(const void* sptr){
	long value;
	if(!sptr){
		printf("value not given");
		return -1;
	}
	const OCTET_STRING_t *st = (const OCTET_STRING_t *)sptr;
	size_t size;
	
	if(!sptr) {
	printf("value not given ")
		return -1;
	}
	
	if(st->size > 0) {
		/* Size in bits */
		size = 8 * st->size - (st->bits_unused & 0x07);
	} else {
		size = 0;
	}
	
	if((size == 18) || (size == 19) || (size == 20)) {
		/* Constraint check succeeded */
		return 0;
	} else {
	printf(" constraint failed ")
		return -1;
	}
}
int Age_constraint(const void* sptr){
	long value;
	if(!sptr){
		printf("value not given");
		return -1;
	}
	value = *(const long *)sptr;
	if ( ( value >= 18 && value <= 20)) {
		return 0;  //constraint success
	}
	else {
		printf("constraint failed");
		return -1;
	}
}
int Month_constraint(const void* sptr){
	long value;
	if(!sptr){
		printf("value not given");
		return -1;
	}
const IA5Strin_t *st = (const IA5Strin_t *)sptr;
	size_t size;
	
	if(!sptr) {
		_ASN_CTFAIL(app_key, td, sptr,
			"%s: value not given (%s:%d)",
			td->name, __FILE__, __LINE__);
		return -1;
	}
	
	size = st->size;
	
	if((size == 5)
		 && !check_permitted_alphabet_2(st)) {
		/* Constraint check succeeded */
		return 0;
	} else {
		printf ("constraint failed");
		return -1;
	}
}
int check_permitted_alphabet_2(const void *sptr) {
	const IA5Strin_t *st = (const IA5Strin_t *)sptr;
	const uint8_t *ch = st->buf;
	const uint8_t *end = ch + st->size;
	
	for(; ch < end; ch++) {
		uint8_t cv = *ch;
		if(!(cv <= 127)) return -1;
	}
	return 0;
}
int Weekend_constraint(const void* sptr){
	long value;
	if(!sptr){
		printf("value not given");
		return -1;
	}
	value = *(const long *)sptr;
	if ( value == 1 || ( value >= 6 && value <= 7) || value == 9) {
		return 0;  //constraint success
	}
	else {
		printf("constraint failed");
		return -1;
	}
}
int Names_constraint(const void* sptr){
	long value;
	if(!sptr){
		printf("value not given");
		return -1;
	}
	size_t size	/* Determine the number of elements */
	size = _A_CSEQUENCE_FROM_VOID(sptr)->count;
	
	if((size == 5)) {
		/* Perform validation of the inner elements */
		return td->check_constraints(td, sptr, ctfailcb, app_key);
	} else {
		printf("constraint failed")
		return -1;
	}
}
int DiskError_constraint(const void* sptr){
	long value;
	if(!sptr){
		printf("value not given");
		return -1;
	}
	const Day_t *st = (const Day_t *)sptr;
	long value;
	
	if(!sptr) {
		_ASN_CTFAIL(app_key, td, sptr,
			"%s: value not given (%s:%d)",
			td->name, __FILE__, __LINE__);
		return -1;
	}	if ( value == disk-full value == 1 ) {
		/* Constraint check succeeded */
		return 0;
	} else {
		printf ("constraint failed");
		return -1;
	}
}
int DiskFull_constraint(const void* sptr){
	long value;
	if(!sptr){
		printf("value not given");
		return -1;
	}
	const Day_t *st = (const Day_t *)sptr;
	long value;
	
	if(!sptr) {
		_ASN_CTFAIL(app_key, td, sptr,
			"%s: value not given (%s:%d)",
			td->name, __FILE__, __LINE__);
		return -1;
	}	if ( value == -1 ) {
		/* Constraint check succeeded */
		return 0;
	} else {
		printf ("constraint failed");
		return -1;
	}
}

