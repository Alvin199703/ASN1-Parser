#define	A_SEQUENCE_OF(type)	A_SET_OF(type)
#define	A_SET_OF(type)
	struct {
		type **array;
		int count;
		int size;
		void (*free)(type *);
	}
typedef struct ASN_PRIMITIVE_TYPE_S {
	uint8_t *buf;
	int size;
}ASN_PRIMITIVE_TYPE_t;
typedef ASN_PRIMITIVE_TYPE_t INTEGER_t;
typedef struct OCTET_STRING {
	uint8_t *buf;
	int size;
}OCTET_STRING_t;
typedef ASN_PRIMITIVE_TYPE_t REAL_t;
typedef int NULL_t;
typedef int BOOLEAN_t;
typedef OCTET_STRING_t utcTime_t
typedef OCTET_STRING_t printableString_t
typedef struct BIT_STRING_s{
	uint8_t *buf;
	int size;
	int bits_unused;
}BIT_STRING_t;
